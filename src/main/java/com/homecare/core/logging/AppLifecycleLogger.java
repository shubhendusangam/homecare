package com.homecare.core.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;

/**
 * Logs key application lifecycle events — startup readiness and shutdown.
 * Includes JVM, OS, profile, and memory information for operational visibility.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppLifecycleLogger {

    private final Environment environment;
    private Instant startedAt;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady(ApplicationReadyEvent event) {
        startedAt = Instant.now();

        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        long heapMax = memory.getHeapMemoryUsage().getMax() / (1024 * 1024);
        long heapUsed = memory.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long uptimeMs = runtime.getUptime();

        String[] profiles = environment.getActiveProfiles();
        String profileStr = profiles.length > 0 ? String.join(", ", profiles) : "default";

        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║                   HomeCare Application Started              ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║  Profiles   : {}", pad(profileStr));
        log.info("║  Java       : {} ({})", pad(System.getProperty("java.version")),
                System.getProperty("java.vendor"));
        log.info("║  OS         : {} {} ({})", System.getProperty("os.name"),
                System.getProperty("os.version"), System.getProperty("os.arch"));
        log.info("║  PID        : {}", pad(String.valueOf(ProcessHandle.current().pid())));
        log.info("║  Heap       : {}MB used / {}MB max", heapUsed, heapMax);
        log.info("║  CPUs       : {}", pad(String.valueOf(Runtime.getRuntime().availableProcessors())));
        log.info("║  Startup    : {}ms", uptimeMs);
        log.info("║  Port       : {}", pad(environment.getProperty("server.port", "8080")));
        log.info("╚══════════════════════════════════════════════════════════════╝");
    }

    @EventListener(ContextClosedEvent.class)
    public void onShutdown(ContextClosedEvent event) {
        String uptime = "N/A";
        if (startedAt != null) {
            Duration d = Duration.between(startedAt, Instant.now());
            uptime = String.format("%dd %dh %dm %ds",
                    d.toDays(), d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart());
        }

        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        long heapUsed = memory.getHeapMemoryUsage().getUsed() / (1024 * 1024);

        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║                HomeCare Application Shutting Down            ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║  Uptime     : {}", pad(uptime));
        log.info("║  Heap used  : {}MB at shutdown", heapUsed);
        log.info("╚══════════════════════════════════════════════════════════════╝");
    }

    private String pad(String value) {
        return value != null ? value : "unknown";
    }
}


