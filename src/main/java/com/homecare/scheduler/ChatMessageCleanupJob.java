package com.homecare.scheduler;

import com.homecare.chat.repository.ChatMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Purges chat messages older than 90 days for completed/cancelled bookings.
 * Fires daily at 3:00 AM, registered in {@link QuartzConfig}.
 */
@Component
@Slf4j
public class ChatMessageCleanupJob implements Job {

    private static final int RETENTION_DAYS = 90;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("ChatMessageCleanupJob — purging messages older than {} days", RETENTION_DAYS);

        Instant cutoff = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        int deleted = chatMessageRepository.deleteOlderThan(cutoff);

        log.info("ChatMessageCleanupJob — purged {} chat messages", deleted);
    }
}

