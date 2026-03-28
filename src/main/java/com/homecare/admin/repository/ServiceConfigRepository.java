package com.homecare.admin.repository;

import com.homecare.admin.entity.ServiceConfig;
import com.homecare.core.enums.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceConfigRepository extends JpaRepository<ServiceConfig, UUID> {

    Optional<ServiceConfig> findByServiceType(ServiceType serviceType);

    List<ServiceConfig> findAllByOrderByServiceTypeAsc();

    List<ServiceConfig> findByActiveTrueOrderByServiceTypeAsc();
}

