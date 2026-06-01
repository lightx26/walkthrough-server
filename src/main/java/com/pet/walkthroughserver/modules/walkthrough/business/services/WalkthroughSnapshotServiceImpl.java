package com.pet.walkthroughserver.modules.walkthrough.business.services;

import com.pet.walkthroughserver.modules.walkthrough.business.models.SnapshotContent;
import com.pet.walkthroughserver.modules.walkthrough.business.util.WalkthroughSnapshotSerializer;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughNotFoundException;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughSnapshotEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughSnapshotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalkthroughSnapshotServiceImpl implements WalkthroughSnapshotService {

    private final WalkthroughSnapshotRepository snapshotRepository;

    @Override
    public void captureSnapshot(WalkthroughEntity walkthrough) {
        if (snapshotRepository.findByWalkthroughIdAndVersion(
                walkthrough.getId(), walkthrough.getVersion()).isPresent()) {
            return;
        }

        SnapshotContent content = WalkthroughSnapshotSerializer.serialize(walkthrough);

        WalkthroughSnapshotEntity snapshot = WalkthroughSnapshotEntity.builder()
                .walkthroughId(walkthrough.getId())
                .version(walkthrough.getVersion())
                .commitSha(walkthrough.getCommitSha())
                .walkthroughContent(content)
                .build();

        snapshotRepository.save(snapshot);
        log.info("Captured snapshot for walkthrough {} version {}", walkthrough.getId(), walkthrough.getVersion());
    }

    @Override
    public SnapshotContent getSnapshotContent(UUID walkthroughId, int version, WalkthroughEntity walkthrough) {
        if (version == walkthrough.getVersion()) {
            return snapshotRepository.findByWalkthroughIdAndVersion(walkthroughId, version)
                    .map(WalkthroughSnapshotEntity::getWalkthroughContent)
                    .orElseGet(() -> WalkthroughSnapshotSerializer.serialize(walkthrough));
        }

        return snapshotRepository.findByWalkthroughIdAndVersion(walkthroughId, version)
                .map(WalkthroughSnapshotEntity::getWalkthroughContent)
                .orElseThrow(() -> new WalkthroughNotFoundException(
                        "Snapshot not found for walkthrough " + walkthroughId + " version " + version));
    }
}
