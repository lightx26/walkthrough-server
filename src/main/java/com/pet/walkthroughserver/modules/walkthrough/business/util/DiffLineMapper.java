package com.pet.walkthroughserver.modules.walkthrough.business.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility for extracting valid line numbers from a unified diff (raw_patch).
 * Used to determine whether annotations anchored to specific lines in one version
 * of a diff are still valid in a new version.
 */
public final class DiffLineMapper {

    private DiffLineMapper() {}

    /**
     * Extracts the set of line numbers present on the given side of a unified diff patch.
     *
     * @param rawPatch the unified diff string
     * @param side     "new" for added/context lines on the new side, "old" for removed/context lines on the old side
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
     * Checks whether a line range [startLine, endLine] is fully present on the given side of the diff.
     */
    public static boolean isRangeValid(String rawPatch, String side, int startLine, int endLine) {
        Set<Integer> validLines = extractLineNumbers(rawPatch, side);
        for (int i = startLine; i <= endLine; i++) {
            if (!validLines.contains(i)) return false;
        }
        return true;
    }

    /**
     * Parses a hunk header like "@@ -10,5 +20,7 @@" to extract old and new starting line numbers.
     * Returns [oldStart, newStart].
     */
    static int[] parseHunkHeader(String header) {
        // Format: @@ -oldStart[,oldCount] +newStart[,newCount] @@
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
                try { oldStart = Integer.parseInt(oldTokens[0]); } catch (NumberFormatException ignored) {}
            }
            if (newTokens.length > 0) {
                try { newStart = Integer.parseInt(newTokens[0]); } catch (NumberFormatException ignored) {}
            }
        }

        return new int[]{oldStart, newStart};
    }
}
