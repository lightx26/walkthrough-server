package com.pet.walkthroughserver.modules.walkthrough.repository;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.pet.walkthroughserver.modules._shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "annotations")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AnnotationEntity extends BaseEntity {

    @JsonBackReference("file-annotations")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "walkthrough_file_id", nullable = false)
    private WalkthroughFileEntity walkthroughFile;

    @Column(name = "start_line", nullable = false)
    private Integer startLine;

    @Column(name = "end_line", nullable = false)
    private Integer endLine;

    @Column(name = "line_side", nullable = false, length = 10)
    private String lineSide;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AnnotationStatus status = AnnotationStatus.ACTIVE;
}
