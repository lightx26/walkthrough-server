package com.pet.walkthroughserver.modules._shared.repository;

import java.util.Optional;
import java.util.function.Supplier;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;

/**
 * Small helpers for the repeated "fetch-or-throw-a-domain-exception" pattern, so that the choice
 * of exception stays at the call site while the unwrapping is uniform.
 */
public final class Repositories {

    private Repositories() {
    }

    /**
     * Return the value held by {@code found}, or throw the supplied domain exception when empty.
     */
    public static <T> T orThrow(Optional<T> found, Supplier<? extends AppException> onMissing) {
        return found.orElseThrow(onMissing);
    }
}
