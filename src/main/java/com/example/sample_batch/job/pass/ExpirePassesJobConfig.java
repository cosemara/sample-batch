package com.example.sample_batch.job.pass;

import com.example.sample_batch.repository.pass.PassEntity;
import com.example.sample_batch.repository.pass.PassStatus;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@RequiredArgsConstructor
@Configuration
public class ExpirePassesJobConfig {

    public static final String JOB_NAME = "expirePassesJob";
    public static final String BEAN_PREFIX = JOB_NAME + "_";

    private final int CHUNK_SIZE = 1;

    private final EntityManagerFactory entityManagerFactory;

    @Bean(JOB_NAME)
    public Job expirePassesJob(final JobRepository jobRepository, @Qualifier(BEAN_PREFIX + "expirePassesStep") final Step expirePassesStep) {
        return new JobBuilder("expirePassesJob", jobRepository)
                                     .start(expirePassesStep)
                                     .build();
    }

    @Bean(BEAN_PREFIX + "expirePassesStep")
    public Step expirePassesStep(final JobRepository jobRepository, final PlatformTransactionManager transactionManager) {
        return new StepBuilder("expirePassesStep", jobRepository)
                                      .<PassEntity, PassEntity>chunk(CHUNK_SIZE, transactionManager)
                                      .reader(expirePassesItemReader())
                                      .processor(expirePassesItemProcessor())
                                      .writer(expirePassesItemWriter())
                                      .build();

    }

    /**
     * JpaCursorItemReader: JpaPagingItemReader만 지원하다가 Spring 4.3에서 추가되었습니다.
     * 페이징 기법보다 보다 높은 성능으로, 데이터 변경에 무관한 무결성 조회가 가능합니다.
     */
    @Bean
    @StepScope
    public JpaCursorItemReader<PassEntity> expirePassesItemReader() {
        return new JpaCursorItemReaderBuilder<PassEntity>()
            .name("expirePassesItemReader")
            .entityManagerFactory(entityManagerFactory)
            // 상태(status)가 진행중이며, 종료일시(endedAt)이 현재 시점보다 과거일 경우 만료 대상이 됩니다.
            .queryString("select p from PassEntity p where p.status = :status and p.endedAt <= :endedAt")
            .parameterValues(Map.of("status", PassStatus.PROGRESSED, "endedAt", LocalDateTime.now()))
            .build();
    }

    @Bean
    public ItemProcessor<PassEntity, PassEntity> expirePassesItemProcessor() {
        return passEntity -> {
            passEntity.setStatus(PassStatus.EXPIRED);
            passEntity.setExpiredAt(LocalDateTime.now());
            return passEntity;
        };
    }

    /**
     * JpaItemWriter: JPA의 영속성 관리를 위해 EntityManager를 필수로 설정해줘야 합니다.
     */
    @Bean
    public JpaItemWriter<PassEntity> expirePassesItemWriter() {
        return new JpaItemWriterBuilder<PassEntity>()
            .entityManagerFactory(entityManagerFactory)
            .build();
    }

}