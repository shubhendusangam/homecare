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
import com.homecare.user.repository.HelperAvailabilitySlotRepository;
import com.homecare.user.repository.HelperUnavailableDateRepository;
import com.homecare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HelperAvailabilityService {

    private final HelperAvailabilitySlotRepository slotRepository;
    private final HelperUnavailableDateRepository unavailableDateRepository;
    private final UserRepository userRepository;

    @Value("${homecare.timezone:Asia/Kolkata}")
    private String timezone;

    // ─── Core availability check ──────────────────────────────────────

    /**
     * Returns true if the helper is available at the given instant.
     * If the helper has no availability slots defined, returns true (backward-compatible).
     */
    public boolean isHelperAvailableAt(UUID helperId, Instant at) {
        ZoneId zone = ZoneId.of(timezone);
        ZonedDateTime zdt = at.atZone(zone);
        LocalDate date = zdt.toLocalDate();
        DayOfWeek day = zdt.getDayOfWeek();
        LocalTime time = zdt.toLocalTime();

        // Check if the helper has any slots defined at all
        List<HelperAvailabilitySlot> allSlots = slotRepository.findByHelperId(helperId);
        if (allSlots.isEmpty()) {
            // No slots defined — treat as always available (backward compatible)
            return true;
        }

        // Check unavailable date override
        if (unavailableDateRepository.existsByHelperIdAndDate(helperId, date)) {
            log.debug("Helper {} is unavailable on {} (marked as unavailable date)", helperId, date);
            return false;
        }

        // Check if any active slot covers the requested time
        List<HelperAvailabilitySlot> daySlots = slotRepository
                .findByHelperIdAndDayOfWeekAndActiveTrue(helperId, day);

        boolean covered = daySlots.stream()
                .anyMatch(slot -> !time.isBefore(slot.getStartTime()) && time.isBefore(slot.getEndTime()));

        if (!covered) {
            log.debug("Helper {} has no active slot covering {} on {}", helperId, time, day);
        }
        return covered;
    }

    // ─── Get weekly schedule ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> getWeeklySchedule(UUID helperId) {
        return slotRepository.findByHelperId(helperId).stream()
                .map(this::toSlotResponse)
                .toList();
    }

    // ─── Set weekly schedule (bulk upsert) ────────────────────────────

    @Transactional
    public List<AvailabilitySlotResponse> setWeeklySchedule(UUID helperId, List<AvailabilitySlotRequest> requests) {
        User helper = userRepository.findById(helperId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", helperId));

        // Validate: startTime < endTime for each slot
        for (AvailabilitySlotRequest req : requests) {
            if (!req.getStartTime().isBefore(req.getEndTime())) {
                throw new BusinessException(
                        "Start time must be before end time for " + req.getDayOfWeek(),
                        ErrorCode.VALIDATION_FAILED);
            }
        }

        // Delete existing slots and insert new ones
        slotRepository.deleteByHelperId(helperId);
        slotRepository.flush();

        List<HelperAvailabilitySlot> newSlots = requests.stream()
                .map(req -> HelperAvailabilitySlot.builder()
                        .helper(helper)
                        .dayOfWeek(req.getDayOfWeek())
                        .startTime(req.getStartTime())
                        .endTime(req.getEndTime())
                        .active(req.isActive())
                        .build())
                .toList();

        List<HelperAvailabilitySlot> saved = slotRepository.saveAll(newSlots);
        log.info("Helper {} weekly schedule updated — {} slots", helperId, saved.size());

        return saved.stream().map(this::toSlotResponse).toList();
    }

    // ─── Unavailable dates ────────────────────────────────────────────

    @Transactional
    public UnavailableDateResponse markDateUnavailable(UUID helperId, UnavailableDateRequest request) {
        User helper = userRepository.findById(helperId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", helperId));

        if (unavailableDateRepository.existsByHelperIdAndDate(helperId, request.getDate())) {
            throw new BusinessException(
                    "Date " + request.getDate() + " is already marked as unavailable",
                    ErrorCode.VALIDATION_FAILED);
        }

        HelperUnavailableDate entity = HelperUnavailableDate.builder()
                .helper(helper)
                .date(request.getDate())
                .reason(request.getReason())
                .build();

        entity = unavailableDateRepository.save(entity);
        log.info("Helper {} marked {} as unavailable (reason: {})", helperId, request.getDate(), request.getReason());

        return toUnavailableDateResponse(entity);
    }

    @Transactional
    public void removeDateUnavailability(UUID helperId, UUID unavailableDateId) {
        HelperUnavailableDate entity = unavailableDateRepository.findById(unavailableDateId)
                .orElseThrow(() -> new ResourceNotFoundException("HelperUnavailableDate", "id", unavailableDateId));

        if (!entity.getHelper().getId().equals(helperId)) {
            throw new BusinessException("This unavailable date does not belong to you", ErrorCode.FORBIDDEN);
        }

        unavailableDateRepository.delete(entity);
        log.info("Helper {} removed unavailable date {} ({})", helperId, entity.getDate(), unavailableDateId);
    }

    @Transactional(readOnly = true)
    public List<UnavailableDateResponse> getUnavailableDates(UUID helperId) {
        return unavailableDateRepository.findByHelperIdAndDateGreaterThanEqual(helperId, LocalDate.now())
                .stream()
                .map(this::toUnavailableDateResponse)
                .toList();
    }

    // ─── Mappers ──────────────────────────────────────────────────────

    private AvailabilitySlotResponse toSlotResponse(HelperAvailabilitySlot slot) {
        return AvailabilitySlotResponse.builder()
                .id(slot.getId())
                .dayOfWeek(slot.getDayOfWeek())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .active(slot.isActive())
                .build();
    }

    private UnavailableDateResponse toUnavailableDateResponse(HelperUnavailableDate entity) {
        return UnavailableDateResponse.builder()
                .id(entity.getId())
                .date(entity.getDate())
                .reason(entity.getReason())
                .build();
    }
}

