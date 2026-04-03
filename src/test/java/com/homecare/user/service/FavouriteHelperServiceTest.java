package com.homecare.user.service;

import com.homecare.booking.repository.BookingRepository;
import com.homecare.core.enums.ErrorCode;
import com.homecare.core.enums.ServiceType;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.user.dto.AddFavouriteRequest;
import com.homecare.user.dto.FavouriteHelperResponse;
import com.homecare.user.entity.FavouriteHelper;
import com.homecare.user.entity.HelperProfile;
import com.homecare.user.entity.User;
import com.homecare.user.enums.HelperStatus;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.FavouriteHelperRepository;
import com.homecare.user.repository.HelperProfileRepository;
import com.homecare.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavouriteHelperService")
class FavouriteHelperServiceTest {

    @Mock private FavouriteHelperRepository favouriteHelperRepository;
    @Mock private UserRepository userRepository;
    @Mock private HelperProfileRepository helperProfileRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private FavouriteHelperService favouriteHelperService;

    private User customer;
    private User helper;
    private HelperProfile helperProfile;
    private FavouriteHelper favourite;

    @BeforeEach
    void setUp() {
        customer = User.builder().name("Customer").email("c@test.com").role(Role.CUSTOMER).build();
        customer.setId(UUID.randomUUID());

        helper = User.builder().name("Helper Meera").email("h@test.com").role(Role.HELPER)
                .avatarUrl("avatar.png").build();
        helper.setId(UUID.randomUUID());

        helperProfile = HelperProfile.builder()
                .user(helper).skills(List.of(ServiceType.CLEANING))
                .status(HelperStatus.ONLINE).available(true)
                .rating(4.8).totalJobsCompleted(15).build();
        helperProfile.setId(UUID.randomUUID());

        favourite = FavouriteHelper.builder()
                .customer(customer).helper(helper)
                .nickname("My Cleaner Meera").notes("Gate code 4521")
                .totalBookingsTogether(3)
                .lastBookedAt(Instant.now().minusSeconds(86400))
                .build();
        favourite.setId(UUID.randomUUID());
    }

    @Nested
    @DisplayName("AddFavourite")
    class AddFavourite {

        @Test
        @DisplayName("should add favourite successfully with uniqueness check")
        void addHappyPath() {
            AddFavouriteRequest request = new AddFavouriteRequest();
            request.setNickname("My Cleaner");
            request.setNotes("Knows the gate code");

            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(userRepository.findById(helper.getId())).thenReturn(Optional.of(helper));
            when(favouriteHelperRepository.existsByCustomerIdAndHelperId(customer.getId(), helper.getId()))
                    .thenReturn(false);
            when(favouriteHelperRepository.save(any(FavouriteHelper.class)))
                    .thenAnswer(inv -> {
                        FavouriteHelper fav = inv.getArgument(0);
                        fav.setId(UUID.randomUUID());
                        return fav;
                    });
            when(helperProfileRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperProfile));
            when(bookingRepository.hasActiveBooking(helper.getId())).thenReturn(false);

            FavouriteHelperResponse result = favouriteHelperService
                    .addFavourite(customer.getId(), helper.getId(), request);

            assertNotNull(result);
            assertEquals(helper.getId(), result.getHelperId());
            assertEquals("Helper Meera", result.getHelperName());
            assertEquals("My Cleaner", result.getNickname());
            assertTrue(result.isAvailableForBooking());
            verify(favouriteHelperRepository).save(any(FavouriteHelper.class));
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("should throw DUPLICATE_FAVOURITE when adding duplicate")
        void addDuplicate() {
            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(userRepository.findById(helper.getId())).thenReturn(Optional.of(helper));
            when(favouriteHelperRepository.existsByCustomerIdAndHelperId(customer.getId(), helper.getId()))
                    .thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> favouriteHelperService.addFavourite(customer.getId(), helper.getId(), null));
            assertEquals(ErrorCode.DUPLICATE_FAVOURITE, ex.getErrorCode());
        }

        @Test
        @DisplayName("should throw when target user is not a HELPER")
        void addNonHelper() {
            User notHelper = User.builder().name("Not Helper").email("nh@test.com").role(Role.CUSTOMER).build();
            notHelper.setId(UUID.randomUUID());

            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(userRepository.findById(notHelper.getId())).thenReturn(Optional.of(notHelper));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> favouriteHelperService.addFavourite(customer.getId(), notHelper.getId(), null));
            assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("ListFavourites")
    class ListFavourites {

        @Test
        @DisplayName("should return favourites with live availability status")
        void listReturnsAvailability() {
            when(favouriteHelperRepository.findByCustomerIdOrderByLastBookedAtDesc(customer.getId()))
                    .thenReturn(List.of(favourite));
            when(helperProfileRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperProfile));
            when(bookingRepository.hasActiveBooking(helper.getId())).thenReturn(false);

            List<FavouriteHelperResponse> result = favouriteHelperService.listFavourites(customer.getId());

            assertEquals(1, result.size());
            FavouriteHelperResponse resp = result.get(0);
            assertEquals(HelperStatus.ONLINE, resp.getCurrentStatus());
            assertTrue(resp.isAvailableForBooking());
            assertEquals(3, resp.getTotalBookingsTogether());
            assertEquals("My Cleaner Meera", resp.getNickname());
        }

        @Test
        @DisplayName("should show unavailable when helper has active booking")
        void listUnavailableHelper() {
            when(favouriteHelperRepository.findByCustomerIdOrderByLastBookedAtDesc(customer.getId()))
                    .thenReturn(List.of(favourite));
            when(helperProfileRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperProfile));
            when(bookingRepository.hasActiveBooking(helper.getId())).thenReturn(true);

            List<FavouriteHelperResponse> result = favouriteHelperService.listFavourites(customer.getId());

            assertFalse(result.get(0).isAvailableForBooking());
        }

        @Test
        @DisplayName("should show OFFLINE helper as unavailable")
        void listOfflineHelper() {
            helperProfile.setStatus(HelperStatus.OFFLINE);
            when(favouriteHelperRepository.findByCustomerIdOrderByLastBookedAtDesc(customer.getId()))
                    .thenReturn(List.of(favourite));
            when(helperProfileRepository.findByUserId(helper.getId()))
                    .thenReturn(Optional.of(helperProfile));

            List<FavouriteHelperResponse> result = favouriteHelperService.listFavourites(customer.getId());

            assertEquals(HelperStatus.OFFLINE, result.get(0).getCurrentStatus());
            assertFalse(result.get(0).isAvailableForBooking());
        }
    }

    @Nested
    @DisplayName("RemoveFavourite")
    class RemoveFavourite {

        @Test
        @DisplayName("should remove favourite successfully")
        void removeHappyPath() {
            when(favouriteHelperRepository.existsByCustomerIdAndHelperId(customer.getId(), helper.getId()))
                    .thenReturn(true);

            favouriteHelperService.removeFavourite(customer.getId(), helper.getId());

            verify(favouriteHelperRepository).deleteByCustomerIdAndHelperId(customer.getId(), helper.getId());
        }

        @Test
        @DisplayName("should throw when favourite does not exist")
        void removeNotFound() {
            UUID randomHelperId = UUID.randomUUID();
            when(favouriteHelperRepository.existsByCustomerIdAndHelperId(customer.getId(), randomHelperId))
                    .thenReturn(false);

            assertThrows(ResourceNotFoundException.class,
                    () -> favouriteHelperService.removeFavourite(customer.getId(), randomHelperId));
        }
    }

    @Nested
    @DisplayName("IncrementBookingsTogether")
    class IncrementBookingsTogether {

        @Test
        @DisplayName("should increment counter and update lastBookedAt")
        void incrementHappyPath() {
            when(favouriteHelperRepository.findByCustomerIdAndHelperId(customer.getId(), helper.getId()))
                    .thenReturn(Optional.of(favourite));
            when(favouriteHelperRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            favouriteHelperService.incrementBookingsTogether(customer, helper);

            verify(favouriteHelperRepository).save(argThat(fav ->
                    fav.getTotalBookingsTogether() == 4 && fav.getLastBookedAt() != null));
        }

        @Test
        @DisplayName("should do nothing if not in favourites")
        void incrementNotFavourite() {
            when(favouriteHelperRepository.findByCustomerIdAndHelperId(customer.getId(), helper.getId()))
                    .thenReturn(Optional.empty());

            favouriteHelperService.incrementBookingsTogether(customer, helper);

            verify(favouriteHelperRepository, never()).save(any());
        }
    }
}

