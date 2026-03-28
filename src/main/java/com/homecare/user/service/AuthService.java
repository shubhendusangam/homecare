package com.homecare.user.service;

import com.homecare.core.enums.ErrorCode;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.logging.AuditEvent;
import com.homecare.user.dto.*;
import com.homecare.user.entity.CustomerProfile;
import com.homecare.user.entity.HelperProfile;
import com.homecare.user.entity.RefreshToken;
import com.homecare.user.entity.User;
import com.homecare.user.enums.HelperStatus;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.CustomerProfileRepository;
import com.homecare.user.repository.HelperProfileRepository;
import com.homecare.user.repository.RefreshTokenRepository;
import com.homecare.user.repository.UserRepository;
import com.homecare.user.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final HelperProfileRepository helperProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LoginResponse registerCustomer(RegisterCustomerRequest request) {
        validateUniqueEmail(request.getEmail());
        validateUniquePhone(request.getPhone());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .active(true)
                .build();
        user = userRepository.save(user);

        CustomerProfile profile = CustomerProfile.builder()
                .user(user)
                .build();
        customerProfileRepository.save(profile);

        log.info("Customer registered: {}", user.getEmail());
        eventPublisher.publishEvent(AuditEvent.of("CUSTOMER_REGISTERED", user.getId(),
                java.util.Map.of("email", user.getEmail())));
        return generateTokens(user);
    }

    @Transactional
    public LoginResponse registerHelper(RegisterHelperRequest request) {
        validateUniqueEmail(request.getEmail());
        validateUniquePhone(request.getPhone());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.HELPER)
                .active(true)
                .build();
        user = userRepository.save(user);

        HelperProfile profile = HelperProfile.builder()
                .user(user)
                .skills(request.getSkills())
                .status(HelperStatus.OFFLINE)
                .build();
        helperProfileRepository.save(profile);

        log.info("Helper registered: {}", user.getEmail());
        eventPublisher.publishEvent(AuditEvent.of("HELPER_REGISTERED", user.getId(),
                java.util.Map.of("email", user.getEmail())));
        return generateTokens(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BusinessException("Invalid email or password", ErrorCode.UNAUTHORIZED));

        if (!user.isActive()) {
            throw new BusinessException("Account is deactivated. Contact support.", ErrorCode.UNAUTHORIZED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("Invalid email or password", ErrorCode.UNAUTHORIZED);
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in: {}", user.getEmail());
        eventPublisher.publishEvent(AuditEvent.of("USER_LOGIN", user.getId(),
                java.util.Map.of("email", user.getEmail(), "role", user.getRole().name())));
        return generateTokens(user);
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        String rawToken = request.getRefreshToken();

        if (!jwtUtil.isValid(rawToken)) {
            throw new BusinessException("Invalid or expired refresh token", ErrorCode.UNAUTHORIZED);
        }

        String tokenHash = hashToken(rawToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new BusinessException("Refresh token not found or revoked", ErrorCode.UNAUTHORIZED));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Refresh token expired", ErrorCode.UNAUTHORIZED);
        }

        // Revoke old token
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        log.info("Token refreshed for user: {}", user.getEmail());
        return generateTokens(user);
    }

    @Transactional
    public void logout(java.util.UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("User logged out, all refresh tokens revoked: {}", userId);
        eventPublisher.publishEvent(AuditEvent.of("USER_LOGOUT", userId));
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .ifPresent(user -> {
                    // TODO: Generate reset token, store it, and send email
                    log.info("Password reset requested for: {}", user.getEmail());
                });
        // Always return success to prevent email enumeration
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // TODO: Validate reset token from DB, find user, update password
        // For now, this is a stub
        log.info("Password reset attempted with token");
        throw new BusinessException("Password reset not yet implemented", ErrorCode.INTERNAL_ERROR);
    }

    // ---- Private helpers ----

    private void validateUniqueEmail(String email) {
        if (userRepository.existsByEmail(email.toLowerCase().trim())) {
            throw new BusinessException("An account with this email already exists", ErrorCode.DUPLICATE_EMAIL);
        }
    }

    private void validateUniquePhone(String phone) {
        if (userRepository.existsByPhone(phone.trim())) {
            throw new BusinessException("An account with this phone number already exists", ErrorCode.VALIDATION_FAILED);
        }
    }

    private LoginResponse generateTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Persist refresh token hash
        RefreshToken rt = RefreshToken.builder()
                .tokenHash(hashToken(refreshToken))
                .user(user)
                .expiresAt(jwtUtil.getExpiration(refreshToken))
                .build();
        refreshTokenRepository.save(rt);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}

