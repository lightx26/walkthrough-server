package com.pet.walkthroughserver.modules.pinnedRepo.business.services;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pet.walkthroughserver.configs.CacheNames;
import com.pet.walkthroughserver.modules.pinnedRepo.repository.PinnedRepoEntity;
import com.pet.walkthroughserver.modules.pinnedRepo.repository.PinnedRepoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PinnedRepoServiceImpl implements PinnedRepoService {

    private final PinnedRepoRepository pinnedRepoRepository;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.PINNED_LIST, key = "#userId"),
            @CacheEvict(value = CacheNames.PINNED_CHECK, key = "#userId + ':' + #repoFullName")
    })
    public PinnedRepoEntity pinRepo(UUID userId, String repoFullName, String repoName, String language) {
        return pinnedRepoRepository.findByUserIdAndRepoFullName(userId, repoFullName)
                .orElseGet(() -> {
                    PinnedRepoEntity entity = PinnedRepoEntity.builder()
                            .userId(userId)
                            .repoFullName(repoFullName)
                            .repoName(repoName)
                            .language(language)
                            .build();
                    return pinnedRepoRepository.save(entity);
                });
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.PINNED_LIST, key = "#userId"),
            @CacheEvict(value = CacheNames.PINNED_CHECK, key = "#userId + ':' + #repoFullName")
    })
    public void unpinRepo(UUID userId, String repoFullName) {
        pinnedRepoRepository.deleteByUserIdAndRepoFullName(userId, repoFullName);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.PINNED_LIST, key = "#userId")
    public List<PinnedRepoEntity> getPinnedRepos(UUID userId) {
        return pinnedRepoRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.PINNED_CHECK, key = "#userId + ':' + #repoFullName")
    public boolean isPinned(UUID userId, String repoFullName) {
        return pinnedRepoRepository.existsByUserIdAndRepoFullName(userId, repoFullName);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> findPinnedFullNames(UUID userId, List<String> repoFullNames) {
        if (repoFullNames == null || repoFullNames.isEmpty()) {
            return Set.of();
        }
        return pinnedRepoRepository.findByUserIdAndRepoFullNameIn(userId, repoFullNames).stream()
                .map(PinnedRepoEntity::getRepoFullName)
                .collect(Collectors.toSet());
    }
}
