package com.homecare.user.service;

import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.user.dto.CustomerProfileDto;
import com.homecare.user.dto.UpdateCustomerRequest;
import com.homecare.user.entity.CustomerProfile;
import com.homecare.user.entity.User;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.CustomerProfileRepository;
import com.homecare.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService")
class CustomerServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerProfileRepository customerProfileRepository;
    @InjectMocks private CustomerService customerService;

    private UUID userId;
    private User user;
    private CustomerProfile profile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .name("John Doe").email("john@example.com").phone("9876543210")
                .role(Role.CUSTOMER).active(true).build();
        user.setId(userId);

        profile = CustomerProfile.builder()
                .user(user).addressLine("123 Main St").city("Delhi").state("Delhi")
                .pincode("110001").latitude(28.6139).longitude(77.2090)
                .preferredLanguage("en").build();
        profile.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("getProfile — happy path")
    void getProfile_happyPath() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(customerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        CustomerProfileDto dto = customerService.getProfile(userId);

        assertEquals(userId, dto.getUserId());
        assertEquals("John Doe", dto.getName());
        assertEquals("Delhi", dto.getCity());
    }

    @Test
    @DisplayName("getProfile — user not found → throws")
    void getProfile_userNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> customerService.getProfile(userId));
    }

    @Test
    @DisplayName("getProfile — profile not found → throws")
    void getProfile_profileNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(customerProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> customerService.getProfile(userId));
    }

    @Test
    @DisplayName("updateProfile — partial update only modifies provided fields")
    void updateProfile_partialUpdate() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(customerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(customerProfileRepository.save(any(CustomerProfile.class))).thenReturn(profile);

        UpdateCustomerRequest req = new UpdateCustomerRequest();
        req.setCity("Mumbai");
        // Name, phone etc are null → should NOT be updated

        CustomerProfileDto dto = customerService.updateProfile(userId, req);

        assertEquals("John Doe", user.getName()); // unchanged
        assertEquals("Mumbai", profile.getCity()); // changed
        assertEquals("Delhi", profile.getState()); // unchanged
    }

    @Test
    @DisplayName("updateProfile — all fields updated")
    void updateProfile_allFields() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(customerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(customerProfileRepository.save(any(CustomerProfile.class))).thenReturn(profile);

        UpdateCustomerRequest req = new UpdateCustomerRequest();
        req.setName("Jane Doe");
        req.setPhone("1234567890");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        req.setPincode("400001");
        req.setLatitude(19.0760);
        req.setLongitude(72.8777);

        customerService.updateProfile(userId, req);

        assertEquals("Jane Doe", user.getName());
        assertEquals("1234567890", user.getPhone());
        assertEquals("Mumbai", profile.getCity());
        assertEquals("Maharashtra", profile.getState());
    }
}

