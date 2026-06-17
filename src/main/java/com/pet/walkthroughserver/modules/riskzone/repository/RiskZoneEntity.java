package com.pet.walkthroughserver.modules.riskzone.repository;

import com.pet.walkthroughserver.modules._shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "risk_zones")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RiskZoneEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_scan_id", nullable = false)
    private RiskScanEntity riskScan;

    @Column(name = "walkthrough_file_id", nullable = false)
    private UUID walkthroughFileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 10)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private RiskCategory category;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "suggestion", columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "start_position")
    private Integer startPosition;

    @Column(name = "end_position")
    private Integer endPosition;

    @Column(name = "line_side", length = 10)
    private String lineSide;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "review_status", nullable = false, length = 20)
    private ReviewStatus reviewStatus = ReviewStatus.OPEN;
}
