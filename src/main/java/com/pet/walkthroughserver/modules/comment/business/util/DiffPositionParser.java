package com.pet.walkthroughserver.modules.comment.business.util;

/**
 * Utility for validating that a stored diff_position exists within a raw unified-diff patch.
 *
 * <p>The diff_position convention matches GitHub's Pull Review Comment "position" field:
 * counting starts at 1 from the first {@code @@} hunk header, and every line
 * (header, context, added {@code +}, deleted {@code -}) increments the counter.
 */
public final class DiffPositionParser {

    private DiffPositionParser() {}

    /**
     * Returns {@code true} if the given {@code diffPosition} falls within the patch.
     * A {@code null} or blank patch is treated as having no valid positions.
     */
    public static boolean isValidPosition(String rawPatch, int diffPosition) {
        if (rawPatch == null || rawPatch.isBlank()) return false;

        int counter = 0;
        boolean started = false;

        for (String line : rawPatch.split("\n")) {
            if (line.startsWith("@@")) {
                started = true;
            }
            if (started) {
                counter++;
                if (counter == diffPosition) return true;
            }
        }
        return false;
    }

    /**
     * Returns the total number of countable lines in the patch (max valid position).
     */
    public static int maxPosition(String rawPatch) {
        if (rawPatch == null || rawPatch.isBlank()) return 0;

        int counter = 0;
        boolean started = false;

        for (String line : rawPatch.split("\n")) {
            if (line.startsWith("@@")) started = true;
            if (started) counter++;
        }
        return counter;
    }
}
