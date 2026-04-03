package com.homecare.user.service;

import com.homecare.booking.repository.BookingRepository;
import com.homecare.core.enums.ErrorCode;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.core.logging.AuditEvent;
import com.homecare.user.dto.AddFavouriteRequest;
import com.homecare.user.dto.FavouriteHelperResponse;
import com.homecare.user.dto.UpdateFavouriteRequest;
import com.homecare.user.entity.FavouriteHelper;
import com.homecare.user.entity.HelperProfile;
import com.homecare.user.entity.User;
import com.homecare.user.enums.HelperStatus;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.FavouriteHelperRepository;
import com.homecare.user.repository.HelperProfileRepository;
import com.homecare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavouriteHelperService {

    private final FavouriteHelperRepository favouriteHelperRepository;
    private final UserRepository userRepository;
    private final HelperProfileRepository helperProfileRepository;
    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ─── Add Favourite ───────────────────────────────────────────────

    @Transactional
    public FavouriteHelperResponse addFavourite(UUID customerId, UUID helperId, AddFavouriteRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", customerId));

        User helperUser = userRepository.findById(helperId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", helperId));

        if (helperUser.getRole() != Role.HELPER) {
            throw new BusinessException("User is not a helper", ErrorCode.VALIDATION_FAILED);
        }

        if (favouriteHelperRepository.existsByCustomerIdAndHelperId(customerId, helperId)) {
            throw new BusinessException("Helper is already in your favourites", ErrorCode.DUPLICATE_FAVOURITE);
        }

        FavouriteHelper favourite = FavouriteHelper.builder()
                .customer(customer)
                .helper(helperUser)
                .nickname(request != null ? request.getNickname() : null)
                .notes(request != null ? request.getNotes() : null)
                .totalBookingsTogether(0)
                .build();
        favourite = favouriteHelperRepository.save(favourite);

        log.info("Customer {} added helper {} to favourites", customerId, helperId);
        eventPublisher.publishEvent(AuditEvent.of("FAVOURITE_ADDED", customerId,
                Map.of("helperId", helperId)));

        return toDto(favourite);
    }

    // ─── Remove Favourite ────────────────────────────────────────────

    @Transactional
    public void removeFavourite(UUID customerId, UUID helperId) {
        if (!favouriteHelperRepository.existsByCustomerIdAndHelperId(customerId, helperId)) {
            throw new ResourceNotFoundException("FavouriteHelper", "helperId", helperId);
        }

        favouriteHelperRepository.deleteByCustomerIdAndHelperId(customerId, helperId);

        log.info("Customer {} removed helper {} from favourites", customerId, helperId);
        eventPublisher.publishEvent(AuditEvent.of("FAVOURITE_REMOVED", customerId,
                Map.of("helperId", helperId)));
    }

    // ─── List Favourites ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FavouriteHelperResponse> listFavourites(UUID customerId) {
        List<FavouriteHelper> favourites = favouriteHelperRepository
                .findByCustomerIdOrderByLastBookedAtDesc(customerId);

        return favourites.stream()
                .map(this::toDto)
                .toList();
    }

    // ─── Update Favourite ────────────────────────────────────────────

    @Transactional
    public FavouriteHelperResponse updateFavourite(UUID customerId, UUID helperId,
                                                    UpdateFavouriteRequest request) {
        FavouriteHelper favourite = favouriteHelperRepository
                .findByCustomerIdAndHelperId(customerId, helperId)
                .orElseThrow(() -> new ResourceNotFoundException("FavouriteHelper", "helperId", helperId));

        if (request.getNickname() != null) {
            favourite.setNickname(request.getNickname());
        }
        if (request.getNotes() != null) {
            favourite.setNotes(request.getNotes());
        }

        favourite = favouriteHelperRepository.save(favourite);
        log.debug("Customer {} updated favourite helper {}", customerId, helperId);

        return toDto(favourite);
    }

    // ─── Increment Bookings Together (called on COMPLETED) ───────────

    @Transactional
    public void incrementBookingsTogether(User customer, User helper) {
        favouriteHelperRepository.findByCustomerIdAndHelperId(customer.getId(), helper.getId())
                .ifPresent(fav -> {
                    fav.setTotalBookingsTogether(fav.getTotalBookingsTogether() + 1);
                    fav.setLastBookedAt(java.time.Instant.now());
                    favouriteHelperRepository.save(fav);
                    log.debug("Incremented bookings together for customer {} / helper {}: now {}",
                            customer.getId(), helper.getId(), fav.getTotalBookingsTogether());
                });
    }

    // ─── DTO Mapping ─────────────────────────────────────────────────

    private FavouriteHelperResponse toDto(FavouriteHelper fav) {
        User helperUser = fav.getHelper();
        HelperProfile profile = helperProfileRepository.findByUserId(helperUser.getId())
                .orElse(null);

        boolean availableForBooking = false;
        HelperStatus currentStatus = HelperStatus.OFFLINE;
        List<com.homecare.core.enums.ServiceType> skills = List.of();
        double rating = 0.0;
        int totalJobsCompleted = 0;

        if (profile != null) {
            currentStatus = profile.getStatus();
            skills = profile.getSkills();
            rating = profile.getRating();
            totalJobsCompleted = profile.getTotalJobsCompleted();
            availableForBooking = profile.getStatus() == HelperStatus.ONLINE
                    && !bookingRepository.hasActiveBooking(helperUser.getId());
        }

        return FavouriteHelperResponse.builder()
                .helperId(helperUser.getId())
                .helperName(helperUser.getName())
                .avatarUrl(helperUser.getAvatarUrl())
                .skills(skills)
                .rating(rating)
                .totalJobsCompleted(totalJobsCompleted)
                .totalBookingsTogether(fav.getTotalBookingsTogether())
                .lastBookedAt(fav.getLastBookedAt())
                .nickname(fav.getNickname())
                .notes(fav.getNotes())
                .currentStatus(currentStatus)
                .availableForBooking(availableForBooking)
                .build();
    }
}

