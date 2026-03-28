package com.homecare.user.service;

import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.user.dto.*;
import com.homecare.user.entity.HelperProfile;
import com.homecare.user.entity.User;
import com.homecare.user.enums.HelperStatus;
import com.homecare.user.repository.HelperProfileRepository;
import com.homecare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HelperService {

    private final UserRepository userRepository;
    private final HelperProfileRepository helperProfileRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public HelperProfileDto getProfile(UUID userId) {
        User user = findUser(userId);
        HelperProfile profile = findProfile(userId);
        return toDto(user, profile);
    }

    @Transactional
    public HelperProfileDto updateProfile(UUID userId, UpdateHelperRequest request) {
        User user = findUser(userId);
        HelperProfile profile = findProfile(userId);

        if (request.getName() != null) user.setName(request.getName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        userRepository.save(user);

        if (request.getSkills() != null && !request.getSkills().isEmpty()) profile.setSkills(request.getSkills());
        if (request.getCity() != null) profile.setCity(request.getCity());
        if (request.getPincode() != null) profile.setPincode(request.getPincode());
        if (request.getIdProofUrl() != null) profile.setIdProofUrl(request.getIdProofUrl());
        helperProfileRepository.save(profile);

        log.info("Helper profile updated: {}", userId);
        return toDto(user, profile);
    }

    @Transactional
    public HelperProfileDto updateStatus(UUID userId, HelperStatusRequest request) {
        User user = findUser(userId);
        HelperProfile profile = findProfile(userId);

        HelperStatus newStatus = request.getStatus();
        HelperStatus oldStatus = profile.getStatus();

        if (newStatus == HelperStatus.ON_JOB) {
            // ON_JOB can only be set by the system, not by the helper directly
            log.warn("Helper {} attempted to set status to ON_JOB directly", userId);
            return toDto(user, profile);
        }

        profile.setStatus(newStatus);
        profile.setAvailable(newStatus == HelperStatus.ONLINE);
        helperProfileRepository.save(profile);

        // Emit WebSocket event
        messagingTemplate.convertAndSend("/topic/helper-status",
                Map.of("helperId", userId.toString(),
                       "oldStatus", oldStatus.name(),
                       "newStatus", newStatus.name()));

        log.info("Helper {} status changed: {} → {}", userId, oldStatus, newStatus);
        return toDto(user, profile);
    }

    @Transactional
    public void updateLocation(UUID userId, LocationUpdateRequest request) {
        HelperProfile profile = findProfile(userId);
        profile.setLatitude(request.getLatitude());
        profile.setLongitude(request.getLongitude());
        profile.setLastLocationUpdate(Instant.now());
        helperProfileRepository.save(profile);

        // Emit location update via WebSocket
        messagingTemplate.convertAndSend("/topic/helper-location",
                Map.of("helperId", userId.toString(),
                       "latitude", request.getLatitude(),
                       "longitude", request.getLongitude()));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private HelperProfile findProfile(UUID userId) {
        return helperProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("HelperProfile", "userId", userId));
    }

    private HelperProfileDto toDto(User user, HelperProfile profile) {
        return HelperProfileDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .skills(profile.getSkills())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .city(profile.getCity())
                .pincode(profile.getPincode())
                .available(profile.isAvailable())
                .backgroundVerified(profile.isBackgroundVerified())
                .idProofUrl(profile.getIdProofUrl())
                .rating(profile.getRating())
                .totalJobsCompleted(profile.getTotalJobsCompleted())
                .status(profile.getStatus())
                .build();
    }
}

