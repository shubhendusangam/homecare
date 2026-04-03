package com.homecare.referral.mapper;

import com.homecare.referral.dto.ReferralEventResponse;
import com.homecare.referral.entity.ReferralEvent;
import org.springframework.stereotype.Component;

@Component
public class ReferralEventResponseMapper {

    public ReferralEventResponse toDto(ReferralEvent event) {
        return ReferralEventResponse.builder()
                .id(event.getId())
                .referrerId(event.getReferrer().getId())
                .referrerName(event.getReferrer().getName())
                .refereeId(event.getReferee().getId())
                .refereeName(event.getReferee().getName())
                .referralCode(event.getReferralCode())
                .status(event.getStatus())
                .signupAt(event.getSignupAt())
                .firstBookingAt(event.getFirstBookingAt())
                .referrerCredit(event.getReferrerCredit())
                .refereeCredit(event.getRefereeCredit())
                .referrerCreditIssued(event.isReferrerCreditIssued())
                .refereeCreditIssued(event.isRefereeCreditIssued())
                .createdAt(event.getCreatedAt())
                .build();
    }
}

