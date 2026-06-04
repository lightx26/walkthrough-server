package com.pet.walkthroughserver.modules.riskzone.repository;

import com.pet.walkthroughserver.modules._shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "risk_scans")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RiskScanEntity extends BaseEntity {

    @Column(name = "walkthrough_id", nullable = false)
    private UUID walkthroughId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private RiskScanStatus status = RiskScanStatus.PENDING;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "total_files", nullable = false)
    @Builder.Default
    private Integer totalFiles = 0;

    @Column(name = "analyzed_files", nullable = false)
    @Builder.Default
    private Integer analyzedFiles = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "file_progress", columnDefinition = "jsonb")
    @Builder.Default
    private List<FileProgressEntry> fileProgress = new ArrayList<>();

    @Column(name = "critical_count", nullable = false)
    @Builder.Default
    private Integer criticalCount = 0;

    @Column(name = "high_count", nullable = false)
    @Builder.Default
    private Integer highCount = 0;

    @Column(name = "medium_count", nullable = false)
    @Builder.Default
    private Integer mediumCount = 0;

    @Column(name = "low_count", nullable = false)
    @Builder.Default
    private Integer lowCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "triggered_by", nullable = false)
    private UUID triggeredBy;

    @OneToMany(mappedBy = "riskScan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RiskZoneEntity> riskZones = new ArrayList<>();
}
