package com.homecare.user.service;

import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.user.dto.CustomerProfileDto;
import com.homecare.user.dto.UpdateCustomerRequest;
import com.homecare.user.entity.CustomerProfile;
import com.homecare.user.entity.User;
import com.homecare.user.repository.CustomerProfileRepository;
import com.homecare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;

    @Transactional(readOnly = true)
    public CustomerProfileDto getProfile(UUID userId) {
        User user = findUser(userId);
        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerProfile", "userId", userId));
        return toDto(user, profile);
    }

    @Transactional
    public CustomerProfileDto updateProfile(UUID userId, UpdateCustomerRequest request) {
        User user = findUser(userId);
        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerProfile", "userId", userId));

        // Update user fields
        if (request.getName() != null) user.setName(request.getName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        userRepository.save(user);

        // Update profile fields
        if (request.getAddressLine() != null) profile.setAddressLine(request.getAddressLine());
        if (request.getCity() != null) profile.setCity(request.getCity());
        if (request.getState() != null) profile.setState(request.getState());
        if (request.getPincode() != null) profile.setPincode(request.getPincode());
        if (request.getLatitude() != null) profile.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) profile.setLongitude(request.getLongitude());
        if (request.getPreferredLanguage() != null) profile.setPreferredLanguage(request.getPreferredLanguage());
        customerProfileRepository.save(profile);

        log.info("Customer profile updated: {}", userId);
        return toDto(user, profile);
    }

    @Transactional(readOnly = true)
    public CustomerProfileDto getAddress(UUID userId) {
        // Returns same DTO — frontend can pick address fields
        return getProfile(userId);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private CustomerProfileDto toDto(User user, CustomerProfile profile) {
        return CustomerProfileDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .addressLine(profile.getAddressLine())
                .city(profile.getCity())
                .state(profile.getState())
                .pincode(profile.getPincode())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .preferredLanguage(profile.getPreferredLanguage())
                .build();
    }
}

