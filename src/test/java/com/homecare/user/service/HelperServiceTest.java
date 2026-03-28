package com.homecare.user.service;

import com.homecare.core.enums.ServiceType;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.user.dto.*;
import com.homecare.user.entity.HelperProfile;
import com.homecare.user.entity.User;
import com.homecare.user.enums.HelperStatus;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.HelperProfileRepository;
import com.homecare.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HelperService")
class HelperServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private HelperProfileRepository helperProfileRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private HelperService helperService;

    private UUID userId;
    private User user;
    private HelperProfile profile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .name("Helper One").email("helper@example.com").phone("9876543210")
                .role(Role.HELPER).active(true).build();
        user.setId(userId);

        profile = HelperProfile.builder()
                .user(user)
                .skills(List.of(ServiceType.CLEANING, ServiceType.COOKING))
                .latitude(28.6139).longitude(77.2090).city("Delhi")
                .available(true).backgroundVerified(true)
                .rating(4.5).totalJobsCompleted(10)
                .status(HelperStatus.ONLINE).build();
        profile.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("getProfile — happy path")
    void getProfile() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(helperProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        HelperProfileDto dto = helperService.getProfile(userId);
        assertEquals("Helper One", dto.getName());
        assertEquals(4.5, dto.getRating());
        assertEquals(HelperStatus.ONLINE, dto.getStatus());
    }

    @Test
    @DisplayName("getProfile — user not found → throws")
    void getProfile_notFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> helperService.getProfile(userId));
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("ONLINE → OFFLINE transitions correctly")
        void onlineToOffline() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(helperProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
            when(helperProfileRepository.save(any(HelperProfile.class))).thenReturn(profile);

            HelperStatusRequest req = new HelperStatusRequest();
            req.setStatus(HelperStatus.OFFLINE);

            HelperProfileDto dto = helperService.updateStatus(userId, req);

            assertEquals(HelperStatus.OFFLINE, profile.getStatus());
            assertFalse(profile.isAvailable());
            verify(messagingTemplate).convertAndSend(eq("/topic/helper-status"), any(Map.class));
        }

        @Test
        @DisplayName("OFFLINE → ONLINE transitions correctly")
        void offlineToOnline() {
            profile.setStatus(HelperStatus.OFFLINE);
            profile.setAvailable(false);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(helperProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
            when(helperProfileRepository.save(any(HelperProfile.class))).thenReturn(profile);

            HelperStatusRequest req = new HelperStatusRequest();
            req.setStatus(HelperStatus.ONLINE);

            helperService.updateStatus(userId, req);

            assertEquals(HelperStatus.ONLINE, profile.getStatus());
            assertTrue(profile.isAvailable());
        }

        @Test
        @DisplayName("ON_JOB status is rejected when set directly")
        void onJobRejected() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(helperProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

            HelperStatusRequest req = new HelperStatusRequest();
            req.setStatus(HelperStatus.ON_JOB);

            HelperProfileDto dto = helperService.updateStatus(userId, req);

            // Status should remain unchanged (ONLINE), not set to ON_JOB
            assertEquals(HelperStatus.ONLINE, profile.getStatus());
            verify(helperProfileRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("updateLocation — updates coordinates and broadcasts")
    void updateLocation() {
        when(helperProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(helperProfileRepository.save(any(HelperProfile.class))).thenReturn(profile);

        LocationUpdateRequest req = new LocationUpdateRequest();
        req.setLatitude(28.7041);
        req.setLongitude(77.1025);

        helperService.updateLocation(userId, req);

        assertEquals(28.7041, profile.getLatitude());
        assertEquals(77.1025, profile.getLongitude());
        assertNotNull(profile.getLastLocationUpdate());
        verify(messagingTemplate).convertAndSend(eq("/topic/helper-location"), any(Map.class));
    }

    @Test
    @DisplayName("updateProfile — partial update")
    void updateProfile() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(helperProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(helperProfileRepository.save(any(HelperProfile.class))).thenReturn(profile);

        UpdateHelperRequest req = new UpdateHelperRequest();
        req.setCity("Mumbai");
        // skills not set → should remain unchanged

        helperService.updateProfile(userId, req);

        assertEquals("Mumbai", profile.getCity());
        assertEquals(2, profile.getSkills().size()); // unchanged
    }
}

