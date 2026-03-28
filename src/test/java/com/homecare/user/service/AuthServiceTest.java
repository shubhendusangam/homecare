package com.homecare.user.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerProfileRepository customerProfileRepository;
    @Mock private HelperProfileRepository helperProfileRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private AuthService authService;

    private RegisterCustomerRequest customerRequest;
    private RegisterHelperRequest helperRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        customerRequest = new RegisterCustomerRequest();
        customerRequest.setName("Test User");
        customerRequest.setEmail("test@example.com");
        customerRequest.setPhone("9876543210");
        customerRequest.setPassword("StrongP@ss1");

        helperRequest = new RegisterHelperRequest();
        helperRequest.setName("Helper User");
        helperRequest.setEmail("helper@example.com");
        helperRequest.setPhone("9876543211");
        helperRequest.setPassword("StrongP@ss1");
        helperRequest.setSkills(List.of(com.homecare.core.enums.ServiceType.CLEANING));

        savedUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .phone("9876543210")
                .passwordHash("encodedPassword")
                .role(Role.CUSTOMER)
                .active(true)
                .build();
        // Simulate JPA setting the ID
        savedUser.setId(UUID.randomUUID());
    }

    // ─── Registration ─────────────────────────────────────────────────

    @Nested
    @DisplayName("registerCustomer")
    class RegisterCustomer {

        @Test
        @DisplayName("happy path — creates user + profile + tokens")
        void happyPath() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhone(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(customerProfileRepository.save(any(CustomerProfile.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(jwtUtil.generateAccessToken(any(), anyString(), any())).thenReturn("access-token");
            when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh-token");
            when(jwtUtil.getExpiration(anyString())).thenReturn(Instant.now().plusSeconds(86400));
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            LoginResponse response = authService.registerCustomer(customerRequest);

            assertNotNull(response);
            assertEquals("access-token", response.getAccessToken());
            assertEquals("refresh-token", response.getRefreshToken());
            assertEquals("CUSTOMER", response.getRole());
            assertEquals("test@example.com", response.getEmail());

            verify(userRepository).save(any(User.class));
            verify(customerProfileRepository).save(any(CustomerProfile.class));
            verify(eventPublisher).publishEvent(any(AuditEvent.class));
        }

        @Test
        @DisplayName("duplicate email → throws BusinessException")
        void duplicateEmail() {
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            assertThrows(BusinessException.class,
                    () -> authService.registerCustomer(customerRequest));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("duplicate phone → throws BusinessException")
        void duplicatePhone() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhone(anyString())).thenReturn(true);

            assertThrows(BusinessException.class,
                    () -> authService.registerCustomer(customerRequest));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("registerHelper")
    class RegisterHelper {

        @Test
        @DisplayName("happy path — creates user + helper profile + tokens")
        void happyPath() {
            User helperUser = User.builder()
                    .name("Helper User").email("helper@example.com").phone("9876543211")
                    .passwordHash("encoded").role(Role.HELPER).active(true).build();
            helperUser.setId(UUID.randomUUID());

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhone(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenReturn(helperUser);
            when(helperProfileRepository.save(any(HelperProfile.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(jwtUtil.generateAccessToken(any(), anyString(), any())).thenReturn("at");
            when(jwtUtil.generateRefreshToken(any())).thenReturn("rt");
            when(jwtUtil.getExpiration(anyString())).thenReturn(Instant.now().plusSeconds(86400));
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            LoginResponse response = authService.registerHelper(helperRequest);

            assertNotNull(response);
            assertEquals("HELPER", response.getRole());
            verify(helperProfileRepository).save(argThat(
                    profile -> profile.getStatus() == HelperStatus.OFFLINE));
        }
    }

    // ─── Login ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("correct credentials → success")
        void correctCredentials() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(savedUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtUtil.generateAccessToken(any(), anyString(), any())).thenReturn("at");
            when(jwtUtil.generateRefreshToken(any())).thenReturn("rt");
            when(jwtUtil.getExpiration(anyString())).thenReturn(Instant.now().plusSeconds(86400));
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("password");

            LoginResponse response = authService.login(request);
            assertNotNull(response);
            assertEquals("at", response.getAccessToken());
        }

        @Test
        @DisplayName("wrong password → throws BusinessException")
        void wrongPassword() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(savedUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("wrong");

            assertThrows(BusinessException.class, () -> authService.login(request));
        }

        @Test
        @DisplayName("email not found → throws BusinessException")
        void emailNotFound() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            LoginRequest request = new LoginRequest();
            request.setEmail("nonexistent@example.com");
            request.setPassword("password");

            assertThrows(BusinessException.class, () -> authService.login(request));
        }

        @Test
        @DisplayName("inactive account → throws BusinessException")
        void inactiveAccount() {
            savedUser.setActive(false);
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(savedUser));

            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("password");

            assertThrows(BusinessException.class, () -> authService.login(request));
        }
    }

    // ─── Refresh ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("valid refresh token → new tokens")
        void validRefreshToken() {
            RefreshToken stored = RefreshToken.builder()
                    .tokenHash("hash")
                    .user(savedUser)
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .revoked(false)
                    .build();

            when(jwtUtil.isValid(anyString())).thenReturn(true);
            when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString()))
                    .thenReturn(Optional.of(stored));
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(jwtUtil.generateAccessToken(any(), anyString(), any())).thenReturn("new-at");
            when(jwtUtil.generateRefreshToken(any())).thenReturn("new-rt");
            when(jwtUtil.getExpiration(anyString())).thenReturn(Instant.now().plusSeconds(86400));

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid-token");

            LoginResponse response = authService.refresh(request);
            assertNotNull(response);
            assertTrue(stored.isRevoked()); // old token should be revoked
        }

        @Test
        @DisplayName("invalid refresh token → throws BusinessException")
        void invalidRefreshToken() {
            when(jwtUtil.isValid(anyString())).thenReturn(false);

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("invalid-token");

            assertThrows(BusinessException.class, () -> authService.refresh(request));
        }

        @Test
        @DisplayName("expired refresh token in DB → throws BusinessException")
        void expiredRefreshTokenInDb() {
            RefreshToken stored = RefreshToken.builder()
                    .tokenHash("hash")
                    .user(savedUser)
                    .expiresAt(Instant.now().minusSeconds(3600))
                    .revoked(false)
                    .build();

            when(jwtUtil.isValid(anyString())).thenReturn(true);
            when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString()))
                    .thenReturn(Optional.of(stored));

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("expired-token");

            assertThrows(BusinessException.class, () -> authService.refresh(request));
        }
    }

    // ─── Logout ───────────────────────────────────────────────────────

    @Test
    @DisplayName("logout revokes all refresh tokens for user")
    void logoutRevokesAllTokens() {
        UUID userId = UUID.randomUUID();
        authService.logout(userId);
        verify(refreshTokenRepository).revokeAllByUserId(userId);
        verify(eventPublisher).publishEvent(any(AuditEvent.class));
    }
}

