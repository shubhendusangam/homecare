package com.homecare.user.entity;

import com.homecare.core.entity.BaseEntity;
import com.homecare.core.enums.ServiceType;
import com.homecare.user.enums.HelperStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "helper_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HelperProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "helper_profile_skills", joinColumns = @JoinColumn(name = "helper_profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "skill")
    @Builder.Default
    private List<ServiceType> skills = new ArrayList<>();

    private double latitude;
    private double longitude;
    private String city;
    private String pincode;

    @Builder.Default
    private boolean available = false;

    @Builder.Default
    private boolean backgroundVerified = false;

    private String idProofUrl;

    @Builder.Default
    private double rating = 0.0;

    @Builder.Default
    private int totalJobsCompleted = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private HelperStatus status = HelperStatus.OFFLINE;

    private Instant lastLocationUpdate;
}

