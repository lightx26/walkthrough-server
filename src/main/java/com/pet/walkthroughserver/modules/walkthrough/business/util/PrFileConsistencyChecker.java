package com.pet.walkthroughserver.modules.walkthrough.business.util;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;

public final class PrFileConsistencyChecker {

    private PrFileConsistencyChecker() {}

    public record Result(boolean consistent, String outdatedReason) {}

    /**
     * Checks whether a walkthrough's file set matches the current PR file list.
     * Both removed files (in walkthrough, gone from PR) and new files (in PR, not in walkthrough)
     * are treated as inconsistencies.
     */
    public static Result check(WalkthroughEntity walkthrough, Set<String> prFilenames) {
        Set<String> walkthroughFilenames = walkthrough.getChapters().stream()
                .flatMap(ch -> ch.getFiles().stream())
                .map(f -> f.getFilename())
                .collect(Collectors.toSet());

        List<String> removedFiles = walkthroughFilenames.stream()
                .filter(f -> !prFilenames.contains(f))
                .sorted()
                .toList();

        List<String> newFiles = prFilenames.stream()
                .filter(f -> !walkthroughFilenames.contains(f))
                .sorted()
                .toList();

        if (removedFiles.isEmpty() && newFiles.isEmpty()) {
            return new Result(true, null);
        }

        return new Result(false, buildReason(removedFiles, newFiles));
    }

    /**
     * Checks a candidate file set (from a publish request) against the current PR file list.
     */
    public static Result check(Set<String> candidateFilenames, Set<String> prFilenames) {
        List<String> removedFiles = candidateFilenames.stream()
                .filter(f -> !prFilenames.contains(f))
                .sorted()
                .toList();

        List<String> newFiles = prFilenames.stream()
                .filter(f -> !candidateFilenames.contains(f))
                .sorted()
                .toList();

        if (removedFiles.isEmpty() && newFiles.isEmpty()) {
            return new Result(true, null);
        }

        return new Result(false, buildReason(removedFiles, newFiles));
    }

    private static String buildReason(List<String> removedFiles, List<String> newFiles) {
        StringBuilder sb = new StringBuilder("This walkthrough is out of date with the current PR.");
        if (!removedFiles.isEmpty()) {
            sb.append("\nFiles removed from PR:\n");
            removedFiles.forEach(f -> sb.append("- ").append(f).append("\n"));
        }
        if (!newFiles.isEmpty()) {
            sb.append("\nNew files in PR not covered by this walkthrough:\n");
            newFiles.forEach(f -> sb.append("- ").append(f).append("\n"));
        }
        sb.append("\nPlease update your chapters to reflect the current PR file set before re-publishing.");
        return sb.toString().strip();
    }
}
