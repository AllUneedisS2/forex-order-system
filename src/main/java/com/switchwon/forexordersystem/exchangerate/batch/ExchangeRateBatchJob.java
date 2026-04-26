package com.switchwon.forexordersystem.exchangerate.batch;

import com.switchwon.forexordersystem.exchangerate.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 환율 수집 Spring Batch Job 정의
 * Tasklet 방식, 한 번의 실행 = 외부 API 1회 호출 + 4개 통화 이력 저장.
 * 실제 비즈니스 로직은 {@link ExchangeRateService#collectAndSave()}
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ExchangeRateBatchJob {

    public static final String JOB_NAME = "exchangeRateCollectJob";
    public static final String STEP_NAME = "exchangeRateCollectStep";

    // 환율 도메인 서비스
    private final ExchangeRateService exchangeRateService;

    // 환율 수집 Job
    @Bean
    public Job exchangeRateCollectJob(JobRepository jobRepository, 
                                      Step exchangeRateCollectStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(exchangeRateCollectStep)
                .build();
    }

    // 환율 수집 Step
    @Bean
    public Step exchangeRateCollectStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .tasklet(exchangeRateCollectTasklet(), transactionManager)
                .build();
    }

    // 환율 수집 Tasklet
    // Service 의 collectAndSave() 호출 위임
    @Bean
    public Tasklet exchangeRateCollectTasklet() {
        return (contribution, chunkContext) -> {
            log.info("[Batch] 환율 수집 Tasklet 시작");
            exchangeRateService.collectAndSave();
            log.info("[Batch] 환율 수집 Tasklet 완료");
            return RepeatStatus.FINISHED;
        };
    }

}