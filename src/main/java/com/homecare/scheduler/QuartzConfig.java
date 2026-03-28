package com.homecare.scheduler;

import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz scheduler configuration — registers recurring cron-triggered jobs.
 * One-shot jobs (BookingAutoExpireJob, ScheduledBookingTriggerJob, BookingReminderJob)
 * are scheduled programmatically by BookingService.
 */
@Configuration
public class QuartzConfig {

    // ─── DailyReportJob — fires every day at 11:55 PM ──────────────────

    @Bean
    public JobDetail dailyReportJobDetail() {
        return JobBuilder.newJob(DailyReportJob.class)
                .withIdentity("dailyReportJob", "reporting")
                .storeDurably()
                .requestRecovery(true)
                .build();
    }

    @Bean
    public Trigger dailyReportTrigger(JobDetail dailyReportJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(dailyReportJobDetail)
                .withIdentity("dailyReportTrigger", "reporting")
                .withSchedule(CronScheduleBuilder
                        .cronSchedule("0 55 23 * * ?")
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }

    // ─── HelperInactivityJob — fires every 30 minutes ──────────────────

    @Bean
    public JobDetail helperInactivityJobDetail() {
        return JobBuilder.newJob(HelperInactivityJob.class)
                .withIdentity("helperInactivityJob", "maintenance")
                .storeDurably()
                .requestRecovery(true)
                .build();
    }

    @Bean
    public Trigger helperInactivityTrigger(JobDetail helperInactivityJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(helperInactivityJobDetail)
                .withIdentity("helperInactivityTrigger", "maintenance")
                .withSchedule(CronScheduleBuilder
                        .cronSchedule("0 0/30 * * * ?")
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }
}

