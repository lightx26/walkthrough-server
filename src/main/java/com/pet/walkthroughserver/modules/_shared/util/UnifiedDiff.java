package com.pet.walkthroughserver.modules._shared.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    // ── Changed-window extraction (used by AI risk detection and chapter suggestion) ──

    /**
     * Extracts windows of changed lines (additions/deletions) with surrounding context.
     * Each window corresponds to one contiguous block of +/- lines in the patch.
     * Diff positions use the same counting convention as {@link #isValidPosition}: starting
     * at 1 from the first {@code @@} header, every line increments the counter.
     *
     * @param rawPatch     unified diff string
     * @param contextLines number of surrounding context lines to include on each side
     * @return list of windows, empty if the patch is blank or has no changed lines
     */
    public static List<DiffWindow> extractChangedWindows(String rawPatch, int contextLines) {
        if (rawPatch == null || rawPatch.isBlank()) return List.of();

        // Phase 1: collect every line with its diff-position and whether it's changed
        record IndexedLine(int position, String content, boolean changed) {}
        List<IndexedLine> indexed = new ArrayList<>();

        int position = 0;
        boolean started = false;
        for (String line : rawPatch.split("\n", -1)) {
            if (line.startsWith("@@")) started = true;
            if (!started) continue;
            position++;
            boolean changed = line.startsWith("+") || line.startsWith("-");
            indexed.add(new IndexedLine(position, line, changed));
        }

        // Phase 2: group consecutive changed lines into blocks, then expand with context
        List<DiffWindow> windows = new ArrayList<>();
        int n = indexed.size();
        int i = 0;
        while (i < n) {
            if (!indexed.get(i).changed()) { i++; continue; }

            // find end of this changed block
            int blockStart = i;
            while (i < n && indexed.get(i).changed()) i++;
            int blockEnd = i - 1; // inclusive

            // expand with context
            int windowStart = Math.max(0, blockStart - contextLines);
            int windowEnd   = Math.min(n - 1, blockEnd + contextLines);

            // determine dominant side from the block
            long additions = 0;
            long deletions = 0;
            for (int j = blockStart; j <= blockEnd; j++) {
                String c = indexed.get(j).content();
                if (c.startsWith("+")) additions++;
                else if (c.startsWith("-")) deletions++;
            }
            String dominantSide = deletions > additions ? "LEFT" : "RIGHT";

            // build text
            StringBuilder sb = new StringBuilder();
            for (int j = windowStart; j <= windowEnd; j++) {
                sb.append(indexed.get(j).content()).append('\n');
            }

            windows.add(new DiffWindow(
                    indexed.get(windowStart).position(),
                    indexed.get(windowEnd).position(),
                    dominantSide,
                    sb.toString()
            ));
        }

        return windows;
    }

    /** A window of changed lines (with context) extracted from a unified diff. */
    public record DiffWindow(
            int startPosition,
            int endPosition,
            String side,
            String text
    ) {}

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
