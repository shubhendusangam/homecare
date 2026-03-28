package com.homecare.tracking.service;

import com.homecare.booking.entity.Booking;
import com.homecare.booking.repository.BookingRepository;
import com.homecare.core.enums.BookingStatus;
import com.homecare.core.enums.ServiceType;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.tracking.dto.LocationBroadcast;
import com.homecare.tracking.dto.LocationUpdate;
import com.homecare.tracking.dto.StatusEvent;
import com.homecare.tracking.entity.LocationHistory;
import com.homecare.tracking.repository.LocationHistoryRepository;
import com.homecare.user.entity.HelperProfile;
import com.homecare.user.entity.User;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.HelperProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrackingService")
class TrackingServiceTest {

    @Mock private LocationHistoryRepository locationHistoryRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private HelperProfileRepository helperProfileRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private TrackingService trackingService;

    private User customer;
    private User helper;
    private HelperProfile helperProfile;
    private Booking booking;

    @BeforeEach
    void setUp() {
        customer = User.builder().name("Customer").email("c@test.com").role(Role.CUSTOMER).build();
        customer.setId(UUID.randomUUID());

        helper = User.builder().name("Helper").email("h@test.com").role(Role.HELPER).build();
        helper.setId(UUID.randomUUID());

        helperProfile = HelperProfile.builder().user(helper).latitude(28.6).longitude(77.2).build();
        helperProfile.setId(UUID.randomUUID());

        booking = Booking.builder()
                .customer(customer).helper(helper)
                .serviceType(ServiceType.CLEANING).status(BookingStatus.HELPER_EN_ROUTE)
                .latitude(28.65).longitude(77.25).build();
        booking.setId(UUID.randomUUID());
    }

    @Nested
    @DisplayName("processLocationUpdate")
    class ProcessLocationUpdate {

        @Test
        @DisplayName("happy path — persists history, updates profile, broadcasts")
        void happyPath() {
            LocationUpdate update = LocationUpdate.builder()
                    .bookingId(booking.getId()).lat(28.62).lng(77.22)
                    .accuracy(10).heading(45).speedKmh(30).build();

            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(locationHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(helperProfileRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperProfile));
            when(helperProfileRepository.save(any())).thenReturn(helperProfile);

            trackingService.processLocationUpdate(helper.getId(), update);

            verify(locationHistoryRepository).save(any(LocationHistory.class));
            verify(helperProfileRepository).save(helperProfile);
            assertEquals(28.62, helperProfile.getLatitude());
            assertEquals(77.22, helperProfile.getLongitude());
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/booking/" + booking.getId() + "/location"), any(LocationBroadcast.class));
            verify(messagingTemplate).convertAndSend(eq("/topic/admin/bookings"), any(LocationBroadcast.class));
        }

        @Test
        @DisplayName("wrong helper → throws TRACKING_NOT_ALLOWED")
        void wrongHelper() {
            UUID otherHelper = UUID.randomUUID();
            LocationUpdate update = LocationUpdate.builder()
                    .bookingId(booking.getId()).lat(28.62).lng(77.22).build();

            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            assertThrows(BusinessException.class,
                    () -> trackingService.processLocationUpdate(otherHelper, update));
        }

        @Test
        @DisplayName("non-trackable status (ASSIGNED) → throws")
        void nonTrackableStatus() {
            booking.setStatus(BookingStatus.ASSIGNED);
            LocationUpdate update = LocationUpdate.builder()
                    .bookingId(booking.getId()).lat(28.62).lng(77.22).build();

            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            assertThrows(BusinessException.class,
                    () -> trackingService.processLocationUpdate(helper.getId(), update));
        }

        @Test
        @DisplayName("booking not found → throws")
        void bookingNotFound() {
            LocationUpdate update = LocationUpdate.builder()
                    .bookingId(UUID.randomUUID()).lat(28.62).lng(77.22).build();

            when(bookingRepository.findById(any())).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> trackingService.processLocationUpdate(helper.getId(), update));
        }
    }

    @Test
    @DisplayName("broadcastStatusChange — sends to correct topic")
    void broadcastStatusChange() {
        trackingService.broadcastStatusChange(booking.getId(), BookingStatus.COMPLETED, "Done");

        verify(messagingTemplate).convertAndSend(
                eq("/topic/booking/" + booking.getId() + "/status"), any(StatusEvent.class));
    }

    @Test
    @DisplayName("getLocationHistory — returns ordered list")
    void getLocationHistory() {
        LocationHistory lh = LocationHistory.builder()
                .booking(booking).helper(helper)
                .latitude(28.62).longitude(77.22)
                .recordedAt(Instant.now()).build();

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(locationHistoryRepository.findByBookingIdOrderByRecordedAtAsc(booking.getId()))
                .thenReturn(List.of(lh));

        List<LocationHistory> result = trackingService.getLocationHistory(booking.getId());
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("computeEta — known distance and speed")
    void computeEta() {
        // Same coordinates → 0 distance → < 1 min
        String eta = trackingService.computeEta(28.6, 77.2, 28.6, 77.2, 30);
        assertEquals("< 1 min", eta);
    }

    @Test
    @DisplayName("computeEta — zero speed uses default")
    void computeEta_zeroSpeed() {
        String eta = trackingService.computeEta(28.6, 77.2, 28.65, 77.25, 0);
        assertNotNull(eta);
        assertTrue(eta.contains("min"));
    }

    @Test
    @DisplayName("getLatestLocation — returns null when no history")
    void getLatestLocation_empty() {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(locationHistoryRepository.findTopByBookingIdOrderByRecordedAtDesc(booking.getId()))
                .thenReturn(Optional.empty());

        LocationBroadcast result = trackingService.getLatestLocation(booking.getId());
        assertNull(result);
    }
}

