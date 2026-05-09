package com.pet.walkthroughserver.modules.starredrepo.business.services;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pet.walkthroughserver.configs.CacheNames;
import com.pet.walkthroughserver.modules.starredrepo.repository.StarredRepoEntity;
import com.pet.walkthroughserver.modules.starredrepo.repository.StarredRepoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StarredRepoServiceImpl implements StarredRepoService {

    private final StarredRepoRepository starredRepoRepository;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.STARRED_LIST, key = "#userId"),
            @CacheEvict(value = CacheNames.STARRED_CHECK, key = "#userId + ':' + #repoFullName")
    })
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
    @Caching(evict = {
            @CacheEvict(value = CacheNames.STARRED_LIST, key = "#userId"),
            @CacheEvict(value = CacheNames.STARRED_CHECK, key = "#userId + ':' + #repoFullName")
    })
    public void unstarRepo(UUID userId, String repoFullName) {
        starredRepoRepository.deleteByUserIdAndRepoFullName(userId, repoFullName);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.STARRED_LIST, key = "#userId")
    public List<StarredRepoEntity> getStarredRepos(UUID userId) {
        return starredRepoRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.STARRED_CHECK, key = "#userId + ':' + #repoFullName")
    public boolean isStarred(UUID userId, String repoFullName) {
        return starredRepoRepository.existsByUserIdAndRepoFullName(userId, repoFullName);
    }
}
