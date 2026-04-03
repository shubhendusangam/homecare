package com.homecare.user.service;

import com.homecare.core.enums.ErrorCode;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.user.dto.AvailabilitySlotRequest;
import com.homecare.user.dto.AvailabilitySlotResponse;
import com.homecare.user.dto.UnavailableDateRequest;
import com.homecare.user.dto.UnavailableDateResponse;
import com.homecare.user.entity.HelperAvailabilitySlot;
import com.homecare.user.entity.HelperUnavailableDate;
import com.homecare.user.entity.User;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.HelperAvailabilitySlotRepository;
import com.homecare.user.repository.HelperUnavailableDateRepository;
import com.homecare.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HelperAvailabilityService")
class HelperAvailabilityServiceTest {

    @Mock private HelperAvailabilitySlotRepository slotRepository;
    @Mock private HelperUnavailableDateRepository unavailableDateRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private HelperAvailabilityService service;

    private User helper;

    @BeforeEach
    void setUp() {
        helper = User.builder().name("Helper").email("h@test.com").role(Role.HELPER).build();
        helper.setId(UUID.randomUUID());
        ReflectionTestUtils.setField(service, "timezone", "Asia/Kolkata");
    }

    // ─── isHelperAvailableAt ──────────────────────────────────────────

    @Nested
    @DisplayName("isHelperAvailableAt")
    class IsHelperAvailableAt {

        @Test
        @DisplayName("returns true when helper has active slot covering the time")
        void withinSlot_returnsTrue() {
            // Monday 10:00 AM IST
            ZonedDateTime mondayMorning = ZonedDateTime.of(2025, 4, 14, 10, 0, 0, 0,
                    ZoneId.of("Asia/Kolkata"));
            Instant at = mondayMorning.toInstant();

            HelperAvailabilitySlot slot = HelperAvailabilitySlot.builder()
                    .helper(helper)
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(18, 0))
                    .active(true)
                    .build();

            when(slotRepository.findByHelperId(helper.getId())).thenReturn(List.of(slot));
            when(unavailableDateRepository.existsByHelperIdAndDate(helper.getId(), mondayMorning.toLocalDate()))
                    .thenReturn(false);
            when(slotRepository.findByHelperIdAndDayOfWeekAndActiveTrue(helper.getId(), DayOfWeek.MONDAY))
                    .thenReturn(List.of(slot));

            assertTrue(service.isHelperAvailableAt(helper.getId(), at));
        }

        @Test
        @DisplayName("returns false when time is outside all slots for that day")
        void outsideSlot_returnsFalse() {
            // Monday 11:00 PM IST — outside 8-18 slot
            ZonedDateTime mondayNight = ZonedDateTime.of(2025, 4, 14, 23, 0, 0, 0,
                    ZoneId.of("Asia/Kolkata"));
            Instant at = mondayNight.toInstant();

            HelperAvailabilitySlot slot = HelperAvailabilitySlot.builder()
                    .helper(helper)
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(18, 0))
                    .active(true)
                    .build();

            when(slotRepository.findByHelperId(helper.getId())).thenReturn(List.of(slot));
            when(unavailableDateRepository.existsByHelperIdAndDate(helper.getId(), mondayNight.toLocalDate()))
                    .thenReturn(false);
            when(slotRepository.findByHelperIdAndDayOfWeekAndActiveTrue(helper.getId(), DayOfWeek.MONDAY))
                    .thenReturn(List.of(slot));

            assertFalse(service.isHelperAvailableAt(helper.getId(), at));
        }

        @Test
        @DisplayName("returns false when unavailable date overrides normal slot")
        void unavailableDate_overridesSlot() {
            // Monday 10:00 AM IST — within slot, but date is unavailable
            ZonedDateTime mondayMorning = ZonedDateTime.of(2025, 4, 14, 10, 0, 0, 0,
                    ZoneId.of("Asia/Kolkata"));
            Instant at = mondayMorning.toInstant();

            HelperAvailabilitySlot slot = HelperAvailabilitySlot.builder()
                    .helper(helper)
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(18, 0))
                    .active(true)
                    .build();

            when(slotRepository.findByHelperId(helper.getId())).thenReturn(List.of(slot));
            when(unavailableDateRepository.existsByHelperIdAndDate(helper.getId(), mondayMorning.toLocalDate()))
                    .thenReturn(true);

            assertFalse(service.isHelperAvailableAt(helper.getId(), at));
        }

        @Test
        @DisplayName("returns true when helper has no slots defined (backward compatible)")
        void noSlotsDefinedReturnsTrue() {
            when(slotRepository.findByHelperId(helper.getId())).thenReturn(List.of());

            assertTrue(service.isHelperAvailableAt(helper.getId(), Instant.now()));
        }

        @Test
        @DisplayName("returns false when day has no active slots")
        void dayWithNoActiveSlots_returnsFalse() {
            // Sunday 10:00 AM IST — only Monday slots defined
            ZonedDateTime sundayMorning = ZonedDateTime.of(2025, 4, 13, 10, 0, 0, 0,
                    ZoneId.of("Asia/Kolkata"));
            Instant at = sundayMorning.toInstant();

            HelperAvailabilitySlot mondaySlot = HelperAvailabilitySlot.builder()
                    .helper(helper)
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(18, 0))
                    .active(true)
                    .build();

            when(slotRepository.findByHelperId(helper.getId())).thenReturn(List.of(mondaySlot));
            when(unavailableDateRepository.existsByHelperIdAndDate(helper.getId(), sundayMorning.toLocalDate()))
                    .thenReturn(false);
            when(slotRepository.findByHelperIdAndDayOfWeekAndActiveTrue(helper.getId(), DayOfWeek.SUNDAY))
                    .thenReturn(List.of());

            assertFalse(service.isHelperAvailableAt(helper.getId(), at));
        }
    }

    // ─── setWeeklySchedule ────────────────────────────────────────────

    @Nested
    @DisplayName("setWeeklySchedule")
    class SetWeeklySchedule {

        @Test
        @DisplayName("deletes old slots and saves new ones")
        void happyPath() {
            AvailabilitySlotRequest req = new AvailabilitySlotRequest();
            req.setDayOfWeek(DayOfWeek.MONDAY);
            req.setStartTime(LocalTime.of(8, 0));
            req.setEndTime(LocalTime.of(18, 0));
            req.setActive(true);

            when(userRepository.findById(helper.getId())).thenReturn(Optional.of(helper));
            when(slotRepository.saveAll(anyList())).thenAnswer(inv -> {
                List<HelperAvailabilitySlot> slots = inv.getArgument(0);
                slots.forEach(s -> s.setId(UUID.randomUUID()));
                return slots;
            });

            List<AvailabilitySlotResponse> result = service.setWeeklySchedule(helper.getId(), List.of(req));

            verify(slotRepository).deleteByHelperId(helper.getId());
            assertEquals(1, result.size());
            assertEquals(DayOfWeek.MONDAY, result.get(0).getDayOfWeek());
        }

        @Test
        @DisplayName("throws when startTime >= endTime")
        void invalidTimeRange() {
            AvailabilitySlotRequest req = new AvailabilitySlotRequest();
            req.setDayOfWeek(DayOfWeek.MONDAY);
            req.setStartTime(LocalTime.of(18, 0));
            req.setEndTime(LocalTime.of(8, 0));
            req.setActive(true);

            when(userRepository.findById(helper.getId())).thenReturn(Optional.of(helper));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.setWeeklySchedule(helper.getId(), List.of(req)));
            assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        }

        @Test
        @DisplayName("throws when helper not found")
        void helperNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.setWeeklySchedule(unknownId, List.of()));
        }
    }

    // ─── markDateUnavailable ──────────────────────────────────────────

    @Nested
    @DisplayName("markDateUnavailable")
    class MarkDateUnavailable {

        @Test
        @DisplayName("saves unavailable date successfully")
        void happyPath() {
            UnavailableDateRequest req = new UnavailableDateRequest();
            req.setDate(LocalDate.of(2025, 4, 15));
            req.setReason("Holiday");

            when(userRepository.findById(helper.getId())).thenReturn(Optional.of(helper));
            when(unavailableDateRepository.existsByHelperIdAndDate(helper.getId(), req.getDate()))
                    .thenReturn(false);
            when(unavailableDateRepository.save(any(HelperUnavailableDate.class))).thenAnswer(inv -> {
                HelperUnavailableDate entity = inv.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

            UnavailableDateResponse response = service.markDateUnavailable(helper.getId(), req);

            assertNotNull(response);
            assertEquals(LocalDate.of(2025, 4, 15), response.getDate());
            assertEquals("Holiday", response.getReason());
        }

        @Test
        @DisplayName("throws on duplicate date")
        void duplicateDate() {
            UnavailableDateRequest req = new UnavailableDateRequest();
            req.setDate(LocalDate.of(2025, 4, 15));

            when(userRepository.findById(helper.getId())).thenReturn(Optional.of(helper));
            when(unavailableDateRepository.existsByHelperIdAndDate(helper.getId(), req.getDate()))
                    .thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.markDateUnavailable(helper.getId(), req));
            assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        }
    }

    // ─── removeDateUnavailability ─────────────────────────────────────

    @Nested
    @DisplayName("removeDateUnavailability")
    class RemoveDateUnavailability {

        @Test
        @DisplayName("removes own unavailable date")
        void happyPath() {
            UUID dateId = UUID.randomUUID();
            HelperUnavailableDate entity = HelperUnavailableDate.builder()
                    .helper(helper)
                    .date(LocalDate.of(2025, 4, 15))
                    .reason("Holiday")
                    .build();
            entity.setId(dateId);

            when(unavailableDateRepository.findById(dateId)).thenReturn(Optional.of(entity));

            assertDoesNotThrow(() -> service.removeDateUnavailability(helper.getId(), dateId));
            verify(unavailableDateRepository).delete(entity);
        }

        @Test
        @DisplayName("throws FORBIDDEN when date belongs to another helper")
        void wrongHelper() {
            UUID dateId = UUID.randomUUID();
            User otherHelper = User.builder().name("Other").email("o@test.com").role(Role.HELPER).build();
            otherHelper.setId(UUID.randomUUID());
            HelperUnavailableDate entity = HelperUnavailableDate.builder()
                    .helper(otherHelper)
                    .date(LocalDate.of(2025, 4, 15))
                    .build();
            entity.setId(dateId);

            when(unavailableDateRepository.findById(dateId)).thenReturn(Optional.of(entity));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.removeDateUnavailability(helper.getId(), dateId));
            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        }
    }
}

