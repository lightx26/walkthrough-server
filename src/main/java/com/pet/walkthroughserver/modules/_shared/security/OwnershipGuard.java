package com.pet.walkthroughserver.modules._shared.security;

import java.util.UUID;
import java.util.function.Supplier;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;

/**
 * Single source of truth for "the acting user must own this resource" checks.
 *
 * <p>The module-specific exception is supplied by the caller, so each module keeps its own
 * access-denied semantics and message while the comparison itself lives in one place.
 */
public final class OwnershipGuard {

    private OwnershipGuard() {
    }

    /**
     * Throw the supplied exception unless {@code actorId} equals {@code ownerId}.
     *
     * @param ownerId  the id of the resource owner (may be {@code null})
     * @param actorId  the id of the acting user
     * @param onDenied supplies the exception to throw when ownership does not match
     */
    public static void require(UUID ownerId, UUID actorId, Supplier<? extends AppException> onDenied) {
        if (ownerId == null || !ownerId.equals(actorId)) {
            throw onDenied.get();
        }
    }
}
