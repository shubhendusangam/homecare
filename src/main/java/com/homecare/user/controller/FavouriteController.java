package com.homecare.user.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.user.dto.AddFavouriteRequest;
import com.homecare.user.dto.FavouriteHelperResponse;
import com.homecare.user.dto.UpdateFavouriteRequest;
import com.homecare.user.security.UserPrincipal;
import com.homecare.user.service.FavouriteHelperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/favourites")
@RequiredArgsConstructor
public class FavouriteController {

    private final FavouriteHelperService favouriteHelperService;

    @PostMapping("/{helperId}")
    public ResponseEntity<ApiResponse<FavouriteHelperResponse>> addFavourite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID helperId,
            @RequestBody(required = false) @Valid AddFavouriteRequest request) {
        FavouriteHelperResponse fav = favouriteHelperService
                .addFavourite(principal.getId(), helperId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(fav));
    }

    @DeleteMapping("/{helperId}")
    public ResponseEntity<ApiResponse<Void>> removeFavourite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID helperId) {
        favouriteHelperService.removeFavourite(principal.getId(), helperId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Helper removed from favourites"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FavouriteHelperResponse>>> listFavourites(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<FavouriteHelperResponse> favourites = favouriteHelperService
                .listFavourites(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(favourites));
    }

    @PatchMapping("/{helperId}")
    public ResponseEntity<ApiResponse<FavouriteHelperResponse>> updateFavourite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID helperId,
            @Valid @RequestBody UpdateFavouriteRequest request) {
        FavouriteHelperResponse fav = favouriteHelperService
                .updateFavourite(principal.getId(), helperId, request);
        return ResponseEntity.ok(ApiResponse.ok(fav, "Favourite updated"));
    }
}

