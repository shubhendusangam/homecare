package com.homecare.scheduler;

import com.homecare.notification.enums.NotificationType;
import com.homecare.notification.service.NotificationService;
import com.homecare.user.entity.HelperProfile;
import com.homecare.user.enums.HelperStatus;
import com.homecare.user.repository.HelperProfileRepository;
import com.homecare.user.service.HelperAvailabilityService;
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
 * Also marks ONLINE helpers as OFFLINE if current time falls outside their availability slot.
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

    @Autowired
    private HelperAvailabilityService helperAvailabilityService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Instant cutoff = Instant.now().minus(INACTIVITY_THRESHOLD_MINUTES, ChronoUnit.MINUTES);
        log.info("HelperInactivityJob — checking for helpers with lastLocationUpdate before {}", cutoff);

        List<HelperProfile> staleHelpers =
                helperProfileRepository.findByStatusAndLastLocationUpdateBefore(HelperStatus.ONLINE, cutoff);

        if (staleHelpers.isEmpty()) {
            log.info("No inactive helpers found");
        } else {
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

            log.info("HelperInactivityJob — marked {} helpers as OFFLINE due to inactivity", staleHelpers.size());
        }

        // Pass 2: check ONLINE helpers outside their availability slot
        checkHelpersOutsideAvailabilitySlot();
    }

    private void checkHelpersOutsideAvailabilitySlot() {
        List<HelperProfile> onlineHelpers =
                helperProfileRepository.findByStatusAndAvailableTrue(HelperStatus.ONLINE);

        Instant now = Instant.now();
        int count = 0;

        for (HelperProfile profile : onlineHelpers) {
            if (!helperAvailabilityService.isHelperAvailableAt(profile.getUser().getId(), now)) {
                profile.setStatus(HelperStatus.OFFLINE);
                profile.setAvailable(false);
                helperProfileRepository.save(profile);

                try {
                    notificationService.sendToUser(
                            profile.getUser().getId(),
                            NotificationType.SYSTEM_ALERT,
                            Map.of("title", "Marked Offline",
                                   "body", "You have been marked offline — outside your availability hours. " +
                                           "Update your availability schedule to stay online."));
                } catch (Exception e) {
                    log.error("Failed to notify helper {} about availability slot: {}",
                            profile.getUser().getId(), e.getMessage());
                }

                log.info("Helper {} auto-set OFFLINE — outside availability slot",
                        profile.getUser().getId());
                count++;
            }
        }

        if (count > 0) {
            log.info("HelperInactivityJob — marked {} helpers as OFFLINE (outside availability slot)", count);
        }
    }
}

