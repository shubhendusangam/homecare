package com.homecare.booking.config;

import com.homecare.admin.service.ServiceConfigCache;
import com.homecare.core.enums.ServiceType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "homecare")
@Data
@Slf4j
public class BookingConfig {

    private List<ServicePricing> services;
    private BookingSettings booking;

    @Autowired(required = false)
    private ServiceConfigCache serviceConfigCache;

    @Data
    public static class ServicePricing {
        private String id;
        private String name;
        private double basePrice;
        private double perHourPrice;
        private String icon;
    }

    @Data
    public static class BookingSettings {
        private double autoAssignRadiusKm = 10;
        private int autoExpireMinutes = 15;
        private int maxAdvanceScheduleDays = 30;
    }

    public ServicePricing getPricingFor(ServiceType serviceType) {
        if (services == null) {
            return null;
        }
        return services.stream()
                .filter(s -> s.getId().equals(serviceType.name()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Computes total price using DB-backed service config (preferred)
     * with YAML fallback.
     */
    public double computeTotalPrice(ServiceType serviceType, int durationHours) {
        // Try DB-backed cache first
        if (serviceConfigCache != null) {
            double price = serviceConfigCache.computeTotalPrice(serviceType, durationHours);
            if (price > 0) return price;
        }
        // Fallback to YAML
        ServicePricing pricing = getPricingFor(serviceType);
        if (pricing == null) {
            log.warn("No pricing configured for service type: {}", serviceType);
            return 0;
        }
        return pricing.getBasePrice() + (durationHours * pricing.getPerHourPrice());
    }

    /**
     * Gets base price using DB-backed service config (preferred)
     * with YAML fallback.
     */
    public double getBasePrice(ServiceType serviceType) {
        if (serviceConfigCache != null) {
            double price = serviceConfigCache.getBasePrice(serviceType);
            if (price > 0) return price;
        }
        ServicePricing pricing = getPricingFor(serviceType);
        return pricing != null ? pricing.getBasePrice() : 0;
    }
}

