package com.homecare.dispute.mapper;

import com.homecare.dispute.dto.DisputeResponse;
import com.homecare.dispute.dto.EvidenceResponse;
import com.homecare.dispute.entity.Dispute;
import com.homecare.dispute.entity.DisputeEvidence;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DisputeResponseMapper {

    public DisputeResponse toDto(Dispute dispute) {
        return toDto(dispute, List.of());
    }

    public DisputeResponse toDto(Dispute dispute, List<DisputeEvidence> evidence) {
        return DisputeResponse.builder()
                .id(dispute.getId())
                .bookingId(dispute.getBooking().getId())
                .raisedById(dispute.getRaisedBy().getId())
                .raisedByName(dispute.getRaisedBy().getName())
                .raisedByRole(dispute.getRaisedByRole())
                .type(dispute.getType())
                .status(dispute.getStatus())
                .description(dispute.getDescription())
                .resolution(dispute.getResolution())
                .adminNotes(dispute.getAdminNotes())
                .assignedAdminId(dispute.getAssignedAdmin() != null ? dispute.getAssignedAdmin().getId() : null)
                .assignedAdminName(dispute.getAssignedAdmin() != null ? dispute.getAssignedAdmin().getName() : null)
                .resolvedAt(dispute.getResolvedAt())
                .refundAmount(dispute.getRefundAmount())
                .evidence(evidence.stream().map(this::toEvidenceDto).toList())
                .createdAt(dispute.getCreatedAt())
                .updatedAt(dispute.getUpdatedAt())
                .build();
    }

    public EvidenceResponse toEvidenceDto(DisputeEvidence evidence) {
        return EvidenceResponse.builder()
                .id(evidence.getId())
                .submittedById(evidence.getSubmittedBy().getId())
                .submittedByName(evidence.getSubmittedBy().getName())
                .type(evidence.getType())
                .content(evidence.getContent())
                .description(evidence.getDescription())
                .createdAt(evidence.getCreatedAt())
                .build();
    }
}

