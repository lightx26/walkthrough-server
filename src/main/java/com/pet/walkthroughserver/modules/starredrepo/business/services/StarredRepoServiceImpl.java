package com.pet.walkthroughserver.modules.starredrepo.business.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pet.walkthroughserver.modules.starredrepo.repository.StarredRepoEntity;
import com.pet.walkthroughserver.modules.starredrepo.repository.StarredRepoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StarredRepoServiceImpl implements StarredRepoService {

    private final StarredRepoRepository starredRepoRepository;

    @Override
    @Transactional
    public StarredRepoEntity starRepo(UUID userId, String repoFullName, String repoName, String language) {
        return starredRepoRepository.findByUserIdAndRepoFullName(userId, repoFullName)
                .orElseGet(() -> {
                    StarredRepoEntity entity = StarredRepoEntity.builder()
                            .userId(userId)
                            .repoFullName(repoFullName)
                            .repoName(repoName)
                            .language(language)
                            .build();
                    return starredRepoRepository.save(entity);
                });
    }

    @Override
    @Transactional
    public void unstarRepo(UUID userId, String repoFullName) {
        starredRepoRepository.deleteByUserIdAndRepoFullName(userId, repoFullName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StarredRepoEntity> getStarredRepos(UUID userId) {
        return starredRepoRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStarred(UUID userId, String repoFullName) {
        return starredRepoRepository.existsByUserIdAndRepoFullName(userId, repoFullName);
    }
}
