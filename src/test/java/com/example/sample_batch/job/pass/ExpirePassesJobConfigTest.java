package com.example.sample_batch.job.pass;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.sample_batch.config.TestBatchConfig;
import com.example.sample_batch.config.TestJobUtil;
import com.example.sample_batch.repository.pass.PassEntity;
import com.example.sample_batch.repository.pass.PassRepository;
import com.example.sample_batch.repository.pass.PassStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {ExpirePassesJobConfig.class, TestBatchConfig.class, TestJobUtil.class})
public class ExpirePassesJobConfigTest {
    @Autowired
    private TestJobUtil testJobUtil;

    @Autowired
    private PassRepository passRepository;

    @Test
    public void test_expirePassesStep() throws Exception {
        // given
        addPassEntities(10);

        // when
        JobExecution jobExecution = testJobUtil.getJobTester("expirePassesJob").launchJob();

        // then
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
        assertEquals("expirePassesJob", jobExecution.getJobInstance().getJobName());

    }

    private void addPassEntities(int size) {
        final LocalDateTime now = LocalDateTime.now();
        final Random random = new Random();

        List<PassEntity> passEntities = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            PassEntity passEntity = new PassEntity();
            passEntity.setPackageSeq(1);
            passEntity.setUserId("A" + 1000000 + i);
            passEntity.setStatus(PassStatus.PROGRESSED);
            passEntity.setRemainingCount(random.nextInt(11));
            passEntity.setStartedAt(now.minusDays(60));
            passEntity.setEndedAt(now.minusDays(1));
            passEntities.add(passEntity);

        }
        passRepository.saveAll(passEntities);

    }

}