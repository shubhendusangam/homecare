package com.homecare.user.service;

import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.user.dto.HelperProfileDto;
import com.homecare.user.dto.UserAdminDto;
import com.homecare.user.entity.HelperProfile;
import com.homecare.user.entity.User;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.HelperProfileRepository;
import com.homecare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAdminService {

    private final UserRepository userRepository;
    private final HelperProfileRepository helperProfileRepository;

    @Transactional(readOnly = true)
    public Page<UserAdminDto> listUsers(Role role, Boolean active, String search, Pageable pageable) {
        return userRepository.findAllWithFilters(role, active, search, pageable)
                .map(this::toAdminDto);
    }

    @Transactional
    public UserAdminDto activateUser(UUID userId) {
        User user = findUser(userId);
        user.setActive(true);
        userRepository.save(user);
        log.info("User activated by admin: {}", userId);
        return toAdminDto(user);
    }

    @Transactional
    public UserAdminDto deactivateUser(UUID userId) {
        User user = findUser(userId);
        user.setActive(false);
        userRepository.save(user);
        log.info("User deactivated by admin: {}", userId);
        return toAdminDto(user);
    }

    @Transactional(readOnly = true)
    public Page<HelperProfileDto> getPendingVerification(Pageable pageable) {
        return helperProfileRepository.findByBackgroundVerifiedFalse(pageable)
                .map(profile -> {
                    User user = profile.getUser();
                    return HelperProfileDto.builder()
                            .userId(user.getId())
                            .name(user.getName())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .skills(profile.getSkills())
                            .city(profile.getCity())
                            .pincode(profile.getPincode())
                            .backgroundVerified(profile.isBackgroundVerified())
                            .idProofUrl(profile.getIdProofUrl())
                            .status(profile.getStatus())
                            .rating(profile.getRating())
                            .totalJobsCompleted(profile.getTotalJobsCompleted())
                            .build();
                });
    }

    @Transactional
    public HelperProfileDto verifyHelper(UUID helperId) {
        HelperProfile profile = helperProfileRepository.findByUserId(helperId)
                .orElseThrow(() -> new ResourceNotFoundException("HelperProfile", "userId", helperId));
        profile.setBackgroundVerified(true);
        helperProfileRepository.save(profile);

        User user = profile.getUser();
        log.info("Helper verified by admin: {}", helperId);

        return HelperProfileDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .skills(profile.getSkills())
                .backgroundVerified(true)
                .status(profile.getStatus())
                .build();
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private UserAdminDto toAdminDto(User user) {
        return UserAdminDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .active(user.isActive())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

