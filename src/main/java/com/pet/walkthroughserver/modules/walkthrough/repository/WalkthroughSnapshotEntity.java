package com.pet.walkthroughserver.modules.walkthrough.repository;

import com.pet.walkthroughserver.modules._shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "walkthrough_snapshots")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WalkthroughSnapshotEntity extends BaseEntity {

    @Column(name = "walkthrough_id", nullable = false)
    private UUID walkthroughId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "commit_sha", length = 255)
    private String commitSha;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "walkthrough_content", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> walkthroughContent;
}
