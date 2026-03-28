package com.homecare.user.entity;

import com.homecare.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String addressLine;
    private String city;
    private String state;
    private String pincode;
    private double latitude;
    private double longitude;

    @Builder.Default
    private String preferredLanguage = "en";
}

