package com.switchwon.forexordersystem.exchangerate.scheduler;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 환율 수집 스케줄러
 * 1. 앱 부팅 완료 직후 1회 (즉시 시세 확보)
 * 2. 매 분 0초마다 (1분 단위 수집)
 * 
 * 스케줄 활성화 여부는 {@code app.scheduler.enabled}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateScheduler {

    // Spring Batch JobLauncher
    private final JobLauncher jobLauncher;

    // 환율 수집 Job (Bean 이름으로, exchangeRateCollectJob)
    private final Job exchangeRateCollectJob;

    // 스케줄러 on/off
    @Value("${app.scheduler.enabled:true}")
    private boolean enabled;

    // 매 분 0초마다 환율 수집 Job 실행
    // cron 식은 yml(app.scheduler.cron)으로 외부화
    @Scheduled(cron = "${app.scheduler.cron:0 * * * * *}")
    public void scheduleCollect() {
        if (!enabled) {
            log.debug("[Scheduler] 비활성 상태 - 정기 수집 skip");
            return;
        }
        runJob("scheduled");
    }

    // 앱 부팅 직후 1회 즉시 실행
    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        if (!enabled) {
            log.info("[Scheduler] 비활성 상태 - 부팅 시 수집 skip");
            return;
        }
        log.info("[Scheduler] 부팅 완료 - 환율 수집 1회 즉시 실행");
        runJob("startup");
    }

    // Job 실행 공통 처리
    private void runJob(String trigger) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("runId", System.currentTimeMillis()) // Job 고유값 지정 => 중복X
                    .addString("trigger", trigger) // 기록 추적용
                    .toJobParameters();
            jobLauncher.run(exchangeRateCollectJob, params);
        } catch (Exception e) {
            log.error("[Scheduler] 환율 수집 Job 실행 실패 (trigger={})", trigger, e);
        }
    }
}