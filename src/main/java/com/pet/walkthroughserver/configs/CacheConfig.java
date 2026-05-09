package com.pet.walkthroughserver.configs;

import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
@SuppressWarnings("removal")
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper cacheObjectMapper = new ObjectMapper();
        cacheObjectMapper.registerModule(new JavaTimeModule());
        cacheObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        cacheObjectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        Jackson2JsonRedisSerializer<Object> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(cacheObjectMapper, Object.class);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.ofEntries(
                // GitHub API - short TTLs due to external data
                Map.entry(CacheNames.GITHUB_REPOS, defaultConfig.entryTtl(Duration.ofMinutes(5))),
                Map.entry(CacheNames.GITHUB_REPO_SEARCH, defaultConfig.entryTtl(Duration.ofMinutes(3))),
                Map.entry(CacheNames.GITHUB_REPO, defaultConfig.entryTtl(Duration.ofMinutes(10))),
                Map.entry(CacheNames.GITHUB_PULLS, defaultConfig.entryTtl(Duration.ofMinutes(2))),
                Map.entry(CacheNames.GITHUB_PULL, defaultConfig.entryTtl(Duration.ofMinutes(3))),
                Map.entry(CacheNames.GITHUB_PR_FILES, defaultConfig.entryTtl(Duration.ofMinutes(10))),
                Map.entry(CacheNames.GITHUB_PR_COMMITS, defaultConfig.entryTtl(Duration.ofMinutes(5))),
                Map.entry(CacheNames.GITHUB_RECENT_PULLS, defaultConfig.entryTtl(Duration.ofMinutes(2))),

                // Walkthrough caches
                Map.entry(CacheNames.WALKTHROUGH_DETAIL, defaultConfig.entryTtl(Duration.ofMinutes(10))),
                Map.entry(CacheNames.WALKTHROUGH_RECENT, defaultConfig.entryTtl(Duration.ofMinutes(2))),
                Map.entry(CacheNames.WALKTHROUGH_COUNT_REPO, defaultConfig.entryTtl(Duration.ofMinutes(3))),
                Map.entry(CacheNames.WALKTHROUGH_COUNT_REPOS, defaultConfig.entryTtl(Duration.ofMinutes(3))),
                Map.entry(CacheNames.WALKTHROUGH_COUNT_PR, defaultConfig.entryTtl(Duration.ofMinutes(3))),
                Map.entry(CacheNames.WALKTHROUGH_COUNT_PRS, defaultConfig.entryTtl(Duration.ofMinutes(3))),
                Map.entry(CacheNames.WALKTHROUGH_COMMENT_COUNTS, defaultConfig.entryTtl(Duration.ofMinutes(2))),
                Map.entry(CacheNames.WALKTHROUGH_PROGRESS, defaultConfig.entryTtl(Duration.ofMinutes(5))),

                // Profile
                Map.entry(CacheNames.PROFILE_STATS, defaultConfig.entryTtl(Duration.ofMinutes(5))),

                // Starred repos
                Map.entry(CacheNames.STARRED_LIST, defaultConfig.entryTtl(Duration.ofMinutes(10))),
                Map.entry(CacheNames.STARRED_CHECK, defaultConfig.entryTtl(Duration.ofMinutes(10))),

                // User
                Map.entry(CacheNames.USER_SEARCH, defaultConfig.entryTtl(Duration.ofMinutes(5)))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
