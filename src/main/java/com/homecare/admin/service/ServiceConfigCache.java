package com.homecare.admin.service;

import com.homecare.admin.entity.ServiceConfig;
import com.homecare.admin.repository.ServiceConfigRepository;
import com.homecare.core.enums.ServiceType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache of service configurations loaded from the {@code service_config} table.
 * Seeded from the DB-backed table. On first startup if the table is empty,
 * a data.sql INSERT or Flyway migration should pre-populate it.
 * Refreshed whenever an admin updates a service configuration.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceConfigCache {

    private final ServiceConfigRepository serviceConfigRepository;

    private final Map<ServiceType, ServiceConfig> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        List<ServiceConfig> dbConfigs = serviceConfigRepository.findAllByOrderByServiceTypeAsc();
        if (dbConfigs.isEmpty()) {
            log.info("ServiceConfigCache — no DB entries yet; seeding defaults");
            seedDefaults();
            dbConfigs = serviceConfigRepository.findAllByOrderByServiceTypeAsc();
        }
        dbConfigs.forEach(sc -> cache.put(sc.getServiceType(), sc));
        log.info("ServiceConfigCache initialized with {} service configs", cache.size());
    }

    /**
     * Seeds default service configs when the table is empty.
     */
    private void seedDefaults() {
        record Seed(ServiceType type, String name, double base, double perHour, String icon) {}
        List<Seed> seeds = List.of(
                new Seed(ServiceType.CLEANING, "Cleaning", 299, 149, "cleaning"),
                new Seed(ServiceType.COOKING, "Cooking", 399, 199, "cooking"),
                new Seed(ServiceType.BABYSITTING, "Babysitting", 499, 249, "babysitting"),
                new Seed(ServiceType.ELDERLY_HELP, "Elderly Help", 449, 199, "elderly")
        );
        for (Seed s : seeds) {
            ServiceConfig config = ServiceConfig.builder()
                    .serviceType(s.type)
                    .name(s.name)
                    .basePrice(s.base)
                    .perHourPrice(s.perHour)
                    .icon(s.icon)
                    .active(true)
                    .build();
            serviceConfigRepository.save(config);
            log.info("Seeded service config: {}", s.type);
        }
    }

    /**
     * Reloads a single entry from the DB into the cache.
     */
    public void refresh(ServiceType serviceType) {
        serviceConfigRepository.findByServiceType(serviceType)
                .ifPresent(sc -> cache.put(sc.getServiceType(), sc));
    }

    /**
     * Full reload from the DB.
     */
    public void refreshAll() {
        cache.clear();
        serviceConfigRepository.findAllByOrderByServiceTypeAsc()
                .forEach(sc -> cache.put(sc.getServiceType(), sc));
        log.info("ServiceConfigCache fully refreshed — {} entries", cache.size());
    }

    public Optional<ServiceConfig> get(ServiceType serviceType) {
        return Optional.ofNullable(cache.get(serviceType));
    }

    public List<ServiceConfig> getAll() {
        return List.copyOf(cache.values());
    }

    public double getBasePrice(ServiceType serviceType) {
        ServiceConfig cfg = cache.get(serviceType);
        return cfg != null ? cfg.getBasePrice() : 0;
    }

    public double computeTotalPrice(ServiceType serviceType, int durationHours) {
        ServiceConfig cfg = cache.get(serviceType);
        if (cfg == null) {
            log.warn("No service config found for {}", serviceType);
            return 0;
        }
        return cfg.getBasePrice() + (durationHours * cfg.getPerHourPrice());
    }
}

