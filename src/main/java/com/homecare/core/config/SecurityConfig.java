package com.homecare.core.config;

import com.homecare.user.security.JwtAccessDeniedHandler;
import com.homecare.user.security.JwtAuthenticationEntryPoint;
import com.homecare.user.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          JwtAccessDeniedHandler jwtAccessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/index.html", "/css/**", "/js/**", "/images/**",
                    "/h2-console/**",
                    "/api/health",
                    "/ws/**"
                ).permitAll()
                .requestMatchers("/api/v1/auth/logout").authenticated()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/bookings/**").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers("/api/v1/customers/**").hasRole("CUSTOMER")
                .requestMatchers("/api/v1/favourites/**").hasRole("CUSTOMER")
                .requestMatchers("/api/v1/referrals/**").hasRole("CUSTOMER")
                .requestMatchers("/api/v1/wallet/**").hasRole("CUSTOMER")
                .requestMatchers("/api/v1/payments/booking/*/pay-wallet").hasRole("CUSTOMER")
                .requestMatchers("/api/v1/payments/booking/*/initiate").hasRole("CUSTOMER")
                .requestMatchers("/api/v1/payments/booking/*/verify").hasRole("CUSTOMER")
                .requestMatchers("/api/v1/payments/booking/*/refund").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/helpers/*/availability").authenticated()
                .requestMatchers("/api/v1/helpers/**").hasRole("HELPER")
                .requestMatchers("/api/v1/tracking/**").authenticated()
                .requestMatchers("/api/v1/notifications/**").authenticated()
                .requestMatchers("/api/v1/chat/**").authenticated()
                .requestMatchers("/api/v1/disputes/**").authenticated()
                .requestMatchers("/api/v1/subscription-plans/**").authenticated()
                .requestMatchers("/api/v1/subscriptions/**").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/v1/reviews").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").authenticated()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
