package com.homecare.user.repository;

import com.homecare.user.entity.FavouriteHelper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavouriteHelperRepository extends JpaRepository<FavouriteHelper, UUID> {

    List<FavouriteHelper> findByCustomerIdOrderByLastBookedAtDesc(UUID customerId);

    Optional<FavouriteHelper> findByCustomerIdAndHelperId(UUID customerId, UUID helperId);

    boolean existsByCustomerIdAndHelperId(UUID customerId, UUID helperId);

    void deleteByCustomerIdAndHelperId(UUID customerId, UUID helperId);

    @Query("SELECT fh.helper.id FROM FavouriteHelper fh WHERE fh.customer.id = :customerId")
    List<UUID> findHelperIdsByCustomerId(@Param("customerId") UUID customerId);
}

