package com.example.sample_batch.job.statistics;

import com.example.sample_batch.repository.booking.BookingEntity;
import com.example.sample_batch.repository.statistics.StatisticsEntity;
import com.example.sample_batch.repository.statistics.StatisticsRepository;
import com.example.sample_batch.util.LocalDateTimeUtils;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MakeStatisticsJobConfig {

    public static final String JOB_NAME = "makeStatisticsJob";
    public static final String BEAN_PREFIX = JOB_NAME + "_";

    private final int CHUNK_SIZE = 10;

    private final EntityManagerFactory entityManagerFactory;
    private final StatisticsRepository statisticsRepository;
    private final MakeDailyStatisticsTasklet makeDailyStatisticsTasklet;
    private final MakeWeeklyStatisticsTasklet makeWeeklyStatisticsTasklet;

    @Bean(name = JOB_NAME)
    public Job makeStatisticsJob(final JobRepository jobRepository,
                                 @Qualifier(BEAN_PREFIX + "addStatisticsStep") final Step addStatisticsStep,
                                 @Qualifier(BEAN_PREFIX + "makeDailyStatisticsStep") final Step makeDailyStatisticsStep,
                                 @Qualifier(BEAN_PREFIX + "makeWeeklyStatisticsStep") final Step makeWeeklyStatisticsStep) {
        Flow addStatisticsFlow = new FlowBuilder<Flow>("addStatisticsFlow")
            .start(addStatisticsStep)
            .build();

        Flow makeDailyStatisticsFlow = new FlowBuilder<Flow>("makeDailyStatisticsFlow")
            .start(makeDailyStatisticsStep)
            .build();

        Flow makeWeeklyStatisticsFlow = new FlowBuilder<Flow>("makeWeeklyStatisticsFlow")
            .start(makeWeeklyStatisticsStep)
            .build();

        Flow parallelMakeStatisticsFlow = new FlowBuilder<Flow>("parallelMakeStatisticsFlow")
            .split(new SimpleAsyncTaskExecutor())
            .add(makeDailyStatisticsFlow, makeWeeklyStatisticsFlow)
            .build();

        return new JobBuilder("makeStatisticsJob", jobRepository)
                                     .start(addStatisticsFlow)
                                     .next(parallelMakeStatisticsFlow)
                                     .build()
                                     .build();
    }

    @Bean(name = BEAN_PREFIX + "addStatisticsStep")
    public Step addStatisticsStep(final JobRepository jobRepository, final PlatformTransactionManager transactionManager) {
        return new StepBuilder("addStatisticsStep", jobRepository)
                                      .<BookingEntity, BookingEntity>chunk(CHUNK_SIZE, transactionManager)
                                      .reader(addStatisticsItemReader(null, null))
                                      .writer(addStatisticsItemWriter())
                                      .build();
    }

    @Bean
    @StepScope
    public JpaCursorItemReader<BookingEntity> addStatisticsItemReader(@Value("#{jobParameters[from]}") String fromString, @Value("#{jobParameters[to]}") String toString) {
        final LocalDateTime from = LocalDateTimeUtils.parse(fromString);
        final LocalDateTime to = LocalDateTimeUtils.parse(toString);

        return new JpaCursorItemReaderBuilder<BookingEntity>()
            .name("usePassesItemReader")
            .entityManagerFactory(entityManagerFactory)
            // JobParameter를 받아 종료 일시(endedAt) 기준으로 통계 대상 예약(Booking)을 조회합니다.
            .queryString("select b from BookingEntity b where b.endedAt between :from and :to")
            .parameterValues(Map.of("from", from, "to", to))
            .build();
    }

    @Bean
    public ItemWriter<BookingEntity> addStatisticsItemWriter() {
        return bookingEntities -> {
            Map<LocalDateTime, StatisticsEntity> statisticsEntityMap = new LinkedHashMap<>();

            for (BookingEntity bookingEntity : bookingEntities) {
                final LocalDateTime statisticsAt = bookingEntity.getStatisticsAt();
                StatisticsEntity statisticsEntity = statisticsEntityMap.get(statisticsAt);

                if (statisticsEntity == null) {
                    statisticsEntityMap.put(statisticsAt, StatisticsEntity.create(bookingEntity));

                } else {
                    statisticsEntity.add(bookingEntity);

                }

            }
            final List<StatisticsEntity> statisticsEntities = new ArrayList<>(statisticsEntityMap.values());
            statisticsRepository.saveAll(statisticsEntities);
            log.info("### addStatisticsStep 종료");

        };
    }

    @Bean(name = BEAN_PREFIX + "makeDailyStatisticsStep")
    public Step makeDailyStatisticsStep(final JobRepository jobRepository, final PlatformTransactionManager transactionManager) {
        return new StepBuilder("makeDailyStatisticsStep", jobRepository)
                                      .tasklet(makeDailyStatisticsTasklet, transactionManager)
                                      .build();
    }

    @Bean(name = BEAN_PREFIX + "makeWeeklyStatisticsStep")
    public Step makeWeeklyStatisticsStep(final JobRepository jobRepository, final PlatformTransactionManager transactionManager) {
        return new StepBuilder("makeWeeklyStatisticsStep", jobRepository)
                                      .tasklet(makeWeeklyStatisticsTasklet, transactionManager)
                                      .build();
    }

}
