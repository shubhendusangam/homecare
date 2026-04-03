package com.homecare.core.config;

import com.homecare.admin.service.ServiceConfigCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Loads seed data from homecare_data.sql on application startup.
 * Skips if data already exists (idempotent).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("dev")
public class DataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final ServiceConfigCache serviceConfigCache;

    @Override
    public void run(ApplicationArguments args) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users", Integer.class);

        if (count != null && count > 0) {
            log.info("Seed data already present ({} users found). Skipping data initialization.", count);
            return;
        }

        log.info("No existing data found. Loading seed data from homecare_data.sql ...");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("homecare_data.sql"));
        populator.setSeparator(";");
        populator.execute(dataSource);
        log.info("Seed data loaded successfully.");

        // Refresh cache since SQL script replaces auto-seeded service configs
        serviceConfigCache.refreshAll();
        log.info("ServiceConfigCache refreshed with seed data.");
    }
}
