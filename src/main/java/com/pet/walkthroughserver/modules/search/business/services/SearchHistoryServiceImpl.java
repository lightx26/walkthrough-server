package com.pet.walkthroughserver.modules.search.business.services;

import com.pet.walkthroughserver.modules.search.repository.SearchHistoryEntity;
import com.pet.walkthroughserver.modules.search.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private static final int MAX_HISTORY = 3;

    private final SearchHistoryRepository searchHistoryRepository;

    @Override
    @Transactional
    public void save(UUID userId, String query) {
        if (query == null || query.isBlank()) return;

        List<SearchHistoryEntity> existing = searchHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // Remove duplicate of the same query (to re-insert it at top)
        existing.stream()
                .filter(h -> h.getQuery().equalsIgnoreCase(query))
                .forEach(searchHistoryRepository::delete);

        // Re-fetch after potential deletion
        List<SearchHistoryEntity> remaining = searchHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // Delete oldest entries beyond the limit
        if (remaining.size() >= MAX_HISTORY) {
            remaining.subList(MAX_HISTORY - 1, remaining.size())
                    .forEach(searchHistoryRepository::delete);
        }

        SearchHistoryEntity entry = SearchHistoryEntity.builder()
                .userId(userId)
                .query(query)
                .build();
        searchHistoryRepository.save(entry);
    }

    @Override
    public List<SearchHistoryEntity> getRecent(UUID userId) {
        List<SearchHistoryEntity> all = searchHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return all.stream().limit(MAX_HISTORY).toList();
    }
}
