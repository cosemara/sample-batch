package com.example.sample_batch.job.pass;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class AddPassesJobConfig {

    public static final String JOB_NAME = "addPassesJob";
    public static final String BEAN_PREFIX = JOB_NAME + "_";

    @Bean(JOB_NAME)
    public Job addPassesJob(final JobRepository jobRepository, @Qualifier(BEAN_PREFIX + "addPassesStep") final Step addPassesStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                                     .start(addPassesStep)
                                     .build();
    }


    @Bean(BEAN_PREFIX+"addPassesStep")
    public Step addPassesStep(final JobRepository jobRepository, final Tasklet addPassesTasklet, final
                              PlatformTransactionManager transactionManager) {
        return new StepBuilder("addPassesStep", jobRepository)
                                      .tasklet(addPassesTasklet, transactionManager)
                                      .build();
    }

}