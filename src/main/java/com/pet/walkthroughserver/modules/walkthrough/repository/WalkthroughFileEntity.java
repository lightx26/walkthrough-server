package com.pet.walkthroughserver.modules.walkthrough.repository;

import com.pet.walkthroughserver.modules._shared.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "walkthrough_files")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WalkthroughFileEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private ChapterEntity chapter;

    @Column(name = "filename", nullable = false, length = 1000)
    private String filename;

    @Column(name = "file_sha", nullable = false)
    private String fileSha;

    @Column(name = "file_status", nullable = false, length = 50)
    private String fileStatus;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "raw_patch", columnDefinition = "TEXT")
    private String rawPatch;

    @OneToMany(mappedBy = "walkthroughFile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<AnnotationEntity> annotations = new ArrayList<>();
}
