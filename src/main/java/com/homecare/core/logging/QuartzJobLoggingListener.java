package com.homecare.core.logging;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Quartz {@link JobListener} that enriches MDC with job correlation context.
 * Every scheduled job execution automatically gets:
 * <ul>
 *   <li><b>jobCorrelationId</b> — unique per-execution ID for tracing</li>
 *   <li><b>jobName</b> — Quartz job name</li>
 *   <li><b>jobGroup</b> — Quartz job group</li>
 * </ul>
 * Registered via {@link com.homecare.scheduler.QuartzConfig}.
 */
@Slf4j
public class QuartzJobLoggingListener implements JobListener {

    private static final String MDC_JOB_CORRELATION_ID = "jobCorrelationId";
    private static final String MDC_JOB_NAME = "jobName";
    private static final String MDC_JOB_GROUP = "jobGroup";

    @Override
    public String getName() {
        return "QuartzJobLoggingListener";
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        String jobName = context.getJobDetail().getKey().getName();
        String jobGroup = context.getJobDetail().getKey().getGroup();
        String correlationId = UUID.randomUUID().toString();

        MDC.put(MDC_JOB_CORRELATION_ID, correlationId);
        MDC.put(MDC_JOB_NAME, jobName);
        MDC.put(MDC_JOB_GROUP, jobGroup);

        log.info("⏱ JOB START — job={} group={} correlationId={}", jobName, jobGroup, correlationId);
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        String jobName = context.getJobDetail().getKey().getName();
        log.warn("⏱ JOB VETOED — job={}", jobName);
        clearMdc();
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        String jobName = context.getJobDetail().getKey().getName();
        long runtimeMs = context.getJobRunTime();

        if (jobException != null) {
            log.error("⏱ JOB FAILED — job={} runtime={}ms error={}",
                    jobName, runtimeMs, jobException.getMessage(), jobException);
        } else if (runtimeMs > 30_000) {
            log.warn("⏱ JOB COMPLETE (SLOW) — job={} runtime={}ms", jobName, runtimeMs);
        } else {
            log.info("⏱ JOB COMPLETE — job={} runtime={}ms", jobName, runtimeMs);
        }

        clearMdc();
    }

    private void clearMdc() {
        MDC.remove(MDC_JOB_CORRELATION_ID);
        MDC.remove(MDC_JOB_NAME);
        MDC.remove(MDC_JOB_GROUP);
    }
}

