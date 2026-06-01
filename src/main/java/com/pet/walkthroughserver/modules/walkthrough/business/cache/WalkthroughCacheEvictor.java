package com.pet.walkthroughserver.modules.walkthrough.business.cache;

import java.util.UUID;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.configs.CacheNames;

import lombok.RequiredArgsConstructor;

/**
 * Single definition of the cache set invalidated by a walkthrough write (create / update / delete).
 *
 * <p>Previously each write method carried its own 6–8 line {@code @Caching(evict = {...})} block.
 * Those blocks had drifted out of sync (e.g. {@code create} did not evict the detail or
 * comment-count caches). Centralising the policy here removes the duplication and the drift:
 * evicting a not-yet-cached detail entry or clearing comment counts on create is a cheap no-op,
 * and is strictly safer than leaving a stale entry behind.
 */
@Component
@RequiredArgsConstructor
public class WalkthroughCacheEvictor {

    private final CacheManager cacheManager;

    /** Evict every cache affected by creating, updating or deleting a walkthrough. */
    public void onWrite(UUID userId, UUID walkthroughId) {
        evict(CacheNames.WALKTHROUGH_DETAIL, walkthroughId);
        evict(CacheNames.WALKTHROUGH_RECENT, userId);
        clear(CacheNames.WALKTHROUGH_COUNT_REPO);
        clear(CacheNames.WALKTHROUGH_COUNT_REPOS);
        clear(CacheNames.WALKTHROUGH_COUNT_PR);
        clear(CacheNames.WALKTHROUGH_COUNT_PRS);
        clear(CacheNames.WALKTHROUGH_COMMENT_COUNTS);
        clear(CacheNames.PROFILE_STATS);
    }

    private void evict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null && key != null) {
            cache.evict(key);
        }
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
