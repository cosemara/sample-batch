package com.example.sample_batch.job.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.sample_batch.config.TestBatchConfig;
import com.example.sample_batch.config.TestJobUtil;
import com.example.sample_batch.repository.booking.BookingEntity;
import com.example.sample_batch.repository.booking.BookingRepository;
import com.example.sample_batch.repository.booking.BookingStatus;
import com.example.sample_batch.repository.pass.PassEntity;
import com.example.sample_batch.repository.pass.PassRepository;
import com.example.sample_batch.repository.pass.PassStatus;
import com.example.sample_batch.repository.user.UserEntity;
import com.example.sample_batch.repository.user.UserRepository;
import com.example.sample_batch.repository.user.UserStatus;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@Slf4j
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {SendNotificationBeforeClassJobConfig.class, TestBatchConfig.class})
public class SendNotificationBeforeClassJobConfigTest {
    @Autowired
    private TestJobUtil testJobUtil;

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private PassRepository passRepository;
    @Autowired
    private UserRepository userRepository;


    @Test
    public void test_addNotificationStep() throws Exception {
        // given
        addBookingEntity();

        // when
        JobExecution jobExecution = testJobUtil.getJobTester("addNotificationStep").launchJob();

        // then
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

    }

    private void addBookingEntity() {
        final LocalDateTime now = LocalDateTime.now();
        final String userId = "A100" + RandomStringUtils.randomNumeric(4);

        UserEntity userEntity = new UserEntity();
        userEntity.setUserId(userId);
        userEntity.setUserName("김영희");
        userEntity.setStatus(UserStatus.ACTIVE);
        userEntity.setPhone("01033334444");
        userEntity.setMeta(Map.of("uuid", "abcd1234"));
        userRepository.save(userEntity);

        PassEntity passEntity = new PassEntity();
        passEntity.setPackageSeq(1);
        passEntity.setUserId(userId);
        passEntity.setStatus(PassStatus.PROGRESSED);
        passEntity.setRemainingCount(10);
        passEntity.setStartedAt(now.minusDays(60));
        passEntity.setEndedAt(now.minusDays(1));
        passRepository.save(passEntity);

        BookingEntity bookingEntity = new BookingEntity();
        bookingEntity.setPassSeq(passEntity.getPassSeq());
        bookingEntity.setUserId(userId);
        bookingEntity.setStatus(BookingStatus.READY);
        bookingEntity.setStartedAt(now.plusMinutes(10));
        bookingEntity.setEndedAt(bookingEntity.getStartedAt().plusMinutes(50));
        bookingRepository.save(bookingEntity);

    }

}