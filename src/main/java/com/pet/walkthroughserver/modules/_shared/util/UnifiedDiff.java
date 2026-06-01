package com.pet.walkthroughserver.modules._shared.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Shared utility for parsing unified-diff (raw_patch) content.
 * Consolidates hunk-header parsing and line-number extraction used
 * by both the comment and walkthrough modules.
 */
public final class UnifiedDiff {

    private UnifiedDiff() {}

    // ── Position validation (used by comment sync) ──

    /**
     * Returns {@code true} if the given {@code diffPosition} falls within the patch.
     * Position counting starts at 1 from the first {@code @@} hunk header, and every
     * line (header, context, added, deleted) increments the counter.
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

    // ── Line-number extraction (used by walkthrough versioning) ──

    /**
     * Extracts the set of line numbers present on the given side of a unified diff patch.
     *
     * @param rawPatch the unified diff string
     * @param side     "new" for added/context lines on the new side,
     *                 "old" for removed/context lines on the old side
     * @return set of 1-based line numbers present on that side
     */
    public static Set<Integer> extractLineNumbers(String rawPatch, String side) {
        Set<Integer> lines = new HashSet<>();
        if (rawPatch == null || rawPatch.isBlank()) return lines;

        int oldLine = 0;
        int newLine = 0;

        for (String line : rawPatch.split("\n")) {
            if (line.startsWith("@@")) {
                int[] parsed = parseHunkHeader(line);
                oldLine = parsed[0];
                newLine = parsed[1];
                continue;
            }

            if (oldLine == 0 && newLine == 0) continue; // before first hunk

            if (line.startsWith("-")) {
                if ("old".equals(side)) {
                    lines.add(oldLine);
                }
                oldLine++;
            } else if (line.startsWith("+")) {
                if ("new".equals(side)) {
                    lines.add(newLine);
                }
                newLine++;
            } else {
                // context line — present on both sides
                if ("old".equals(side)) {
                    lines.add(oldLine);
                } else {
                    lines.add(newLine);
                }
                oldLine++;
                newLine++;
            }
        }

        return lines;
    }

    /**
     * Checks whether a line range [startLine, endLine] is fully present on the given side.
     */
    public static boolean isRangeValid(String rawPatch, String side, int startLine, int endLine) {
        Set<Integer> validLines = extractLineNumbers(rawPatch, side);
        for (int i = startLine; i <= endLine; i++) {
            if (!validLines.contains(i)) return false;
        }
        return true;
    }

    // ── Shared hunk-header parser ──

    /**
     * Parses a hunk header like "{@code @@ -10,5 +20,7 @@}" to extract starting line numbers.
     *
     * @return {@code [oldStart, newStart]}
     */
    static int[] parseHunkHeader(String header) {
        int oldStart = 1;
        int newStart = 1;

        int minusIdx = header.indexOf('-');
        int plusIdx = header.indexOf('+', minusIdx);
        int endIdx = header.indexOf("@@", 2);

        if (minusIdx >= 0 && plusIdx >= 0 && endIdx >= 0) {
            String oldPart = header.substring(minusIdx + 1, plusIdx).trim().replace(",", " ");
            String newPart = header.substring(plusIdx + 1, endIdx).trim().replace(",", " ");

            String[] oldTokens = oldPart.split("\\s+");
            String[] newTokens = newPart.split("\\s+");

            if (oldTokens.length > 0) {
                try { oldStart = Integer.parseInt(oldTokens[0]); } catch (NumberFormatException e) { /* malformed hunk */ }
            }
            if (newTokens.length > 0) {
                try { newStart = Integer.parseInt(newTokens[0]); } catch (NumberFormatException e) { /* malformed hunk */ }
            }
        }

        return new int[]{oldStart, newStart};
    }
}
