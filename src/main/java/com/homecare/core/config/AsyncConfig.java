package com.homecare.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configures a dedicated thread pool for async notification delivery (email, SMS).
 * <p>
 * The {@code notificationExecutor} is used by {@code AsyncNotificationDispatcher}
 * to run email and SMS sends off the request thread, keeping booking API response
 * times low (&lt; 100ms).
 * <p>
 * {@link ThreadPoolExecutor.CallerRunsPolicy} ensures that if the queue is full,
 * the send executes on the caller thread rather than being silently dropped.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notif-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

