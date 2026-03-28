package com.homecare.scheduler;

import com.homecare.notification.enums.NotificationType;
import com.homecare.notification.service.NotificationService;
import com.homecare.user.entity.HelperProfile;
import com.homecare.user.enums.HelperStatus;
import com.homecare.user.repository.HelperProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Fires every 30 minutes.
 * Marks helpers as OFFLINE if {@code lastLocationUpdate} is older than 60 minutes
 * and status = ONLINE.
 * Sends {@code SYSTEM_ALERT} notification to those helpers.
 */
@Component
@Slf4j
public class HelperInactivityJob implements Job {

    private static final int INACTIVITY_THRESHOLD_MINUTES = 60;

    @Autowired
    private HelperProfileRepository helperProfileRepository;

    @Autowired
    private NotificationService notificationService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Instant cutoff = Instant.now().minus(INACTIVITY_THRESHOLD_MINUTES, ChronoUnit.MINUTES);
        log.info("HelperInactivityJob — checking for helpers with lastLocationUpdate before {}", cutoff);

        List<HelperProfile> staleHelpers =
                helperProfileRepository.findByStatusAndLastLocationUpdateBefore(HelperStatus.ONLINE, cutoff);

        if (staleHelpers.isEmpty()) {
            log.info("No inactive helpers found");
            return;
        }

        for (HelperProfile profile : staleHelpers) {
            profile.setStatus(HelperStatus.OFFLINE);
            profile.setAvailable(false);
            helperProfileRepository.save(profile);

            try {
                notificationService.sendToUser(
                        profile.getUser().getId(),
                        NotificationType.SYSTEM_ALERT,
                        Map.of("title", "Marked Offline",
                               "body", "You have been marked offline due to inactivity. " +
                                       "Please update your location to go back online."));
            } catch (Exception e) {
                log.error("Failed to notify helper {} about inactivity: {}",
                        profile.getUser().getId(), e.getMessage());
            }

            log.info("Helper {} marked OFFLINE due to inactivity (last update: {})",
                    profile.getUser().getId(), profile.getLastLocationUpdate());
        }

        log.info("HelperInactivityJob — marked {} helpers as OFFLINE", staleHelpers.size());
    }
}

