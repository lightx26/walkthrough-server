package com.pet.walkthroughserver.modules.pinnedRepo.repository;

import com.pet.walkthroughserver.modules._shared.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "starred_repos")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PinnedRepoEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private java.util.UUID userId;

    @Column(name = "repo_full_name", nullable = false)
    private String repoFullName;

    @Column(name = "repo_name", nullable = false)
    private String repoName;

    @Column(name = "language")
    private String language;
}
