package com.homecare.user.repository;

import com.homecare.user.entity.HelperUnavailableDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HelperUnavailableDateRepository extends JpaRepository<HelperUnavailableDate, UUID> {

    boolean existsByHelperIdAndDate(UUID helperId, LocalDate date);

    List<HelperUnavailableDate> findByHelperId(UUID helperId);

    List<HelperUnavailableDate> findByHelperIdAndDateGreaterThanEqual(UUID helperId, LocalDate fromDate);
}

