package com.pet.walkthroughserver.modules.profile.business.services;

import com.pet.walkthroughserver.modules.profile.business.models.PinnedWalkthrough;
import com.pet.walkthroughserver.modules.profile.exceptions.AlreadyPinnedException;
import com.pet.walkthroughserver.modules.profile.exceptions.PinLimitExceededException;
import com.pet.walkthroughserver.modules.profile.exceptions.PinNotFoundException;
import com.pet.walkthroughserver.modules.profile.presentation.dto.PinWalkthroughRequest;
import com.pet.walkthroughserver.modules.profile.presentation.dto.ReorderPinsRequest;
import com.pet.walkthroughserver.modules.profile.repository.WalkthroughPinEntity;
import com.pet.walkthroughserver.modules.profile.repository.WalkthroughPinRepository;
import com.pet.walkthroughserver.modules.user.exceptions.UserNotFoundException;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import com.pet.walkthroughserver.modules.user.repository.UserRepository;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughAccessDeniedException;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughNotFoundException;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalkthroughPinServiceImpl implements WalkthroughPinService {

    private static final int MAX_PINS = 6;

    private final WalkthroughPinRepository pinRepository;
    private final WalkthroughRepository walkthroughRepository;
    private final UserRepository userRepository;

    @Override
    public List<PinnedWalkthrough> getPins(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        List<WalkthroughPinEntity> pins = pinRepository.findByUserIdOrderBySortOrderAsc(user.getId());
        List<UUID> walkthroughIds = pins.stream().map(WalkthroughPinEntity::getWalkthroughId).toList();
        Map<UUID, WalkthroughEntity> walkthroughMap = walkthroughRepository.findAllById(walkthroughIds)
                .stream().collect(Collectors.toMap(WalkthroughEntity::getId, Function.identity()));

        return pins.stream()
                .filter(pin -> walkthroughMap.containsKey(pin.getWalkthroughId()))
                .map(pin -> toPinnedWalkthrough(pin, walkthroughMap.get(pin.getWalkthroughId())))
                .toList();
    }

    @Override
    @Transactional
    public PinnedWalkthrough pinWalkthrough(UUID userId, PinWalkthroughRequest request) {
        WalkthroughEntity walkthrough = walkthroughRepository.findById(request.getWalkthroughId())
                .orElseThrow(() -> new WalkthroughNotFoundException("Walkthrough not found"));

        if (!walkthrough.getUserId().equals(userId)) {
            throw new WalkthroughAccessDeniedException("Only the walkthrough author can pin it");
        }

        if (pinRepository.findByUserIdAndWalkthroughId(userId, request.getWalkthroughId()).isPresent()) {
            throw new AlreadyPinnedException();
        }

        long currentCount = pinRepository.countByUserId(userId);
        if (currentCount >= MAX_PINS) {
            throw new PinLimitExceededException();
        }

        WalkthroughPinEntity pin = WalkthroughPinEntity.builder()
                .userId(userId)
                .walkthroughId(request.getWalkthroughId())
                .sortOrder((int) currentCount)
                .build();

        pin = pinRepository.save(pin);
        return toPinnedWalkthrough(pin, walkthrough);
    }

    @Override
    @Transactional
    public void reorderPins(UUID userId, ReorderPinsRequest request) {
        List<WalkthroughPinEntity> existingPins = pinRepository.findByUserIdOrderBySortOrderAsc(userId);
        Set<UUID> existingIds = existingPins.stream()
                .map(WalkthroughPinEntity::getId)
                .collect(Collectors.toSet());

        Set<UUID> requestIds = Set.copyOf(request.getPinIds());

        if (!existingIds.equals(requestIds)) {
            throw new IllegalArgumentException("Pin IDs must match existing pins exactly");
        }

        Map<UUID, WalkthroughPinEntity> pinMap = existingPins.stream()
                .collect(Collectors.toMap(WalkthroughPinEntity::getId, Function.identity()));

        for (int i = 0; i < request.getPinIds().size(); i++) {
            WalkthroughPinEntity pin = pinMap.get(request.getPinIds().get(i));
            pin.setSortOrder(i);
        }

        pinRepository.saveAll(existingPins);
    }

    @Override
    @Transactional
    public void unpinWalkthrough(UUID userId, UUID pinId) {
        WalkthroughPinEntity pin = pinRepository.findById(pinId)
                .orElseThrow(PinNotFoundException::new);

        if (!pin.getUserId().equals(userId)) {
            throw new WalkthroughAccessDeniedException("Cannot unpin another user's pin");
        }

        pinRepository.delete(pin);

        // Re-compact sort orders
        List<WalkthroughPinEntity> remaining = pinRepository.findByUserIdOrderBySortOrderAsc(userId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setSortOrder(i);
        }
        pinRepository.saveAll(remaining);
    }

    private PinnedWalkthrough toPinnedWalkthrough(WalkthroughPinEntity pin, WalkthroughEntity wt) {
        return new PinnedWalkthrough(
                pin.getId(),
                wt.getId(),
                wt.getTitle(),
                wt.getOwner(),
                wt.getRepo(),
                wt.getPrNumber(),
                wt.getStatus(),
                pin.getSortOrder(),
                pin.getCreatedAt());
    }
}
