package com.homecare.user.repository;

import com.homecare.user.entity.HelperAvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public interface HelperAvailabilitySlotRepository extends JpaRepository<HelperAvailabilitySlot, UUID> {

    List<HelperAvailabilitySlot> findByHelperId(UUID helperId);

    List<HelperAvailabilitySlot> findByHelperIdAndDayOfWeekAndActiveTrue(UUID helperId, DayOfWeek dayOfWeek);

    void deleteByHelperId(UUID helperId);
}

