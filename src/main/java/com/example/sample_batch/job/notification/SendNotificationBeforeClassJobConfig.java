package com.example.sample_batch.job.notification;

import com.example.sample_batch.repository.booking.BookingEntity;
import com.example.sample_batch.repository.booking.BookingStatus;
import com.example.sample_batch.repository.notification.NotificationEntity;
import com.example.sample_batch.repository.notification.NotificationEvent;
import com.example.sample_batch.repository.notification.NotificationModelMapper;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@RequiredArgsConstructor
@Configuration
public class SendNotificationBeforeClassJobConfig {

    public static final String JOB_NAME = "sendNotificationBeforeClassJob";
    public static final String BEAN_PREFIX = JOB_NAME + "_";

    private final int CHUNK_SIZE = 10;

    private final EntityManagerFactory entityManagerFactory;
    private final SendNotificationItemWriter sendNotificationItemWriter;

    @Bean(JOB_NAME)
    public Job sendNotificationBeforeClassJob(final JobRepository jobRepository, @Qualifier(BEAN_PREFIX + "addNotificationStep") final Step addNotificationStep, @Qualifier(BEAN_PREFIX + "sendNotificationStep") final Step sendNotificationStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                                     .start(addNotificationStep)
                                     .next(sendNotificationStep)
                                     .build();
    }

    @Bean(BEAN_PREFIX+"addNotificationStep")
    public Step addNotificationStep(final JobRepository jobRepository, final PlatformTransactionManager transactionManager) {
        return new StepBuilder("addNotificationStep", jobRepository)
                                      .<BookingEntity, NotificationEntity>chunk(CHUNK_SIZE, transactionManager)
                                      .reader(addNotificationItemReader())
                                      .processor(addNotificationItemProcessor())
                                      .writer(addNotificationItemWriter())
                                      .build();

    }

    /**
     * JpaPagingItemReader: JPA에서 사용하는 페이징 기법입니다.
     * 쿼리 당 pageSize만큼 가져오며 다른 PagingItemReader와 마찬가지로 Thread-safe 합니다.
     */
    @Bean
    public JpaPagingItemReader<BookingEntity> addNotificationItemReader() {
        return new JpaPagingItemReaderBuilder<BookingEntity>()
            .name("addNotificationItemReader")
            .entityManagerFactory(entityManagerFactory)
            // pageSize: 한 번에 조회할 row 수
            .pageSize(CHUNK_SIZE)
            // 상태(status)가 준비중이며, 시작일시(startedAt)이 10분 후 시작하는 예약이 알람 대상이 됩니다.
            .queryString("select b from BookingEntity b join fetch b.userEntity where b.status = :status and b.startedAt <= :startedAt order by b.bookingSeq")
            .parameterValues(Map.of("status", BookingStatus.READY, "startedAt", LocalDateTime.now().plusMinutes(10)))
            .build();
    }

    @Bean
    public ItemProcessor<BookingEntity, NotificationEntity> addNotificationItemProcessor() {
        return bookingEntity -> NotificationModelMapper.INSTANCE.toNotificationEntity(bookingEntity, NotificationEvent.BEFORE_CLASS);
    }

    @Bean
    public JpaItemWriter<NotificationEntity> addNotificationItemWriter() {
        return new JpaItemWriterBuilder<NotificationEntity>()
            .entityManagerFactory(entityManagerFactory)
            .build();
    }

    /**
     * reader는 synchrosized로 순차적으로 실행되지만 writer는 multi-thread 로 동작합니다.
     */
    @Bean(BEAN_PREFIX+"sendNotificationStep")
    public Step sendNotificationStep(final JobRepository jobRepository, final PlatformTransactionManager transactionManager) {
        return new StepBuilder("sendNotificationStep", jobRepository)
            .<NotificationEntity, NotificationEntity>chunk(CHUNK_SIZE, transactionManager)
            .reader(sendNotificationItemReader())
            .writer(sendNotificationItemWriter)
            .taskExecutor(new SimpleAsyncTaskExecutor())
            .build();
    }

    /**
     * SynchronizedItemStreamReader: multi-thread 환경에서 reader와 writer는 thread-safe 해야합니다.
     * Cursor 기법의 ItemReader는 thread-safe하지 않아 Paging 기법을 사용하거나 synchronized 를 선언하여 순차적으로 수행해야합니다.
     */
    @Bean
    public SynchronizedItemStreamReader<NotificationEntity> sendNotificationItemReader() {
        JpaCursorItemReader<NotificationEntity> itemReader = new JpaCursorItemReaderBuilder<NotificationEntity>()
            .name("sendNotificationItemReader")
            .entityManagerFactory(entityManagerFactory)
            // 이벤트(event)가 수업 전이며, 발송 여부(sent)가 미발송인 알람이 조회 대상이 됩니다.
            .queryString("select n from NotificationEntity n where n.event = :event and n.sent = :sent")
            .parameterValues(Map.of("event", NotificationEvent.BEFORE_CLASS, "sent", false))
            .build();

        return new SynchronizedItemStreamReaderBuilder<NotificationEntity>()
            .delegate(itemReader)
            .build();

    }

}