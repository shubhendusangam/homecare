package com.homecare.dispute.entity;

import com.homecare.core.entity.BaseEntity;
import com.homecare.core.enums.EvidenceType;
import com.homecare.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dispute_evidence", indexes = {
        @Index(name = "idx_evidence_dispute", columnList = "dispute_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeEvidence extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false)
    private Dispute dispute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id", nullable = false)
    private User submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvidenceType type;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(length = 500)
    private String description;
}

