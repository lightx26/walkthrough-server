package com.pet.walkthroughserver.modules.auth.business.job;

import com.pet.walkthroughserver.modules.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredOrRevokedTokens() {
        int deleted = refreshTokenRepository.deleteAllExpiredOrRevoked(Instant.now());
        log.info("Token cleanup: deleted {} expired or revoked refresh token(s)", deleted);
    }
}
