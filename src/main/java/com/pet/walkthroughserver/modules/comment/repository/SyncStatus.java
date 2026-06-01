package com.pet.walkthroughserver.modules.comment.repository;

/**
 * Lifecycle of a comment's synchronization to GitHub.
 *
 * <p>Persisted (and exposed on the API) as a lowercase string via {@link SyncStatusConverter},
 * preserving the existing {@code sync_status} column values and API contract.
 */
public enum SyncStatus {

    PENDING,
    SYNCED,
    FAILED,
    PERMANENTLY_FAILED;

    /** The lowercase wire/storage value, e.g. {@code PERMANENTLY_FAILED -> "permanently_failed"}. */
    public String dbValue() {
        return name().toLowerCase();
    }

    /** Parse a stored/wire value back into the enum. */
    public static SyncStatus fromDbValue(String value) {
        return SyncStatus.valueOf(value.toUpperCase());
    }
}
