package com.pet.walkthroughserver.modules.riskzone.business.prompt;

import com.pet.walkthroughserver.modules._shared.infra.ai.LlmRequest;
import com.pet.walkthroughserver.modules._shared.util.UnifiedDiff.DiffWindow;
import com.pet.walkthroughserver.modules.riskzone.business.models.ChapterContext;
import com.pet.walkthroughserver.modules.riskzone.business.models.FileChangeSummary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RiskPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are a senior code reviewer specializing in identifying risks in code changes.
            Analyze the provided unified diff windows and identify risks in the following categories:
            - RACE_CONDITION: concurrent access issues, missing locks, non-atomic operations
            - BREAKING_CHANGE: public API changes, removed/renamed methods, signature changes
            - MISSING_ROLLBACK: forward-only migrations, missing transaction rollback, no error recovery
            - DATA_INTEGRITY: null safety issues, missing constraints, potential data corruption
            - INPUT_VALIDATION: removed or bypassed input validation, missing sanitization
            - AUTH_AUTHZ: modified authentication/authorization checks, security bypasses
            - ERROR_HANDLING: deleted error handling, swallowed exceptions, removed try/catch
            - BUSINESS_LOGIC: changed core business rules that could break existing behavior
            - REMOVED_TESTS: deleted or disabled test cases
            - OTHER: any other significant risk not covered above

            Use the chapter context (title, description, and the list of files changed together)
            to understand the intent of the change. It is provided only as background — report
            risks for the FILE UNDER REVIEW, anchoring positions to that file's diff windows.

            Respond with a single JSON object (no markdown fences) with exactly two fields:

            {
              "risks": [
                {
                  "risk_level": "LOW" | "MEDIUM" | "HIGH" | "CRITICAL",
                  "category": one of the category names above,
                  "title": "Short title (max 80 chars)",
                  "description": "Detailed explanation of the risk",
                  "suggestion": "Concrete remediation suggestion",
                  "start_position": <integer diff position where risk starts, or null>,
                  "end_position": <integer diff position where risk ends, or null>,
                  "line_side": "LEFT" | "RIGHT" | null
                }
              ],
              "change_summary": {
                "added":    ["each exported symbol, public method, or API endpoint added"],
                "removed":  ["each exported symbol, public method, or API endpoint removed"],
                "modified": ["each exported symbol, public method, or API endpoint changed, with a brief note on what changed"]
              }
            }

            Use an empty array for "risks" if no risks are found. Only include exported/public
            symbols, method signatures, interface contracts, schema changes, and API surface changes
            in "change_summary" — omit internal/private implementation details.
            Only return the JSON object, nothing else.
            """;

    private static final String CROSS_FILE_SYSTEM_PROMPT = """
            You are a senior code reviewer performing a CROSS-FILE integration review of a single
            chapter of related changes. You are given the chapter intent, the list of files changed
            together, the exported/public API changes of each file, and the per-file risks already found.

            Your job is to find risks that only become visible when looking across files together,
            for example:
            - a caller changed in one file but its callee/contract in another file was not updated
            - a signature/return-type/enum change in one file with stale usages in sibling files
            - migrations or schema changes not matched by corresponding code changes (or vice versa)
            - inconsistent validation, auth, or error handling across files that should agree
            - removed code in one file still referenced by another

            Do NOT repeat the per-file risks already listed. Only report NEW cross-file risks.

            Respond with a JSON array of risk objects. Each object must have:
            {
              "filename": "the file this risk primarily manifests in (must be one of the listed files)",
              "risk_level": "LOW" | "MEDIUM" | "HIGH" | "CRITICAL",
              "category": one of the category names above,
              "title": "Short title (max 80 chars)",
              "description": "Detailed explanation, naming the other file(s) involved",
              "suggestion": "Concrete remediation suggestion",
              "start_position": null,
              "end_position": null,
              "line_side": null
            }

            Return an empty array [] if no cross-file risks are found. Do not include markdown fences.
            Only return the JSON array, nothing else.
            """;

    /**
     * Per-file (map) prompt. Prepends a chapter-context block so the model understands the
     * intent of the change and which sibling files participate, then the file's diff windows.
     */
    public LlmRequest build(String filename, String fileStatus, List<DiffWindow> windows,
                            ChapterContext context, int maxContextChars) {
        StringBuilder user = new StringBuilder();

        appendContext(user, context, filename, maxContextChars);

        user.append("=== File under review ===\n");
        user.append("File: ").append(filename).append('\n');
        user.append("Status: ").append(fileStatus).append('\n');
        user.append("Changed windows:\n\n");

        int idx = 1;
        for (DiffWindow w : windows) {
            user.append("--- Window ").append(idx++).append(" ---\n");
            user.append("Diff positions: ").append(w.startPosition())
                    .append("–").append(w.endPosition())
                    .append(" (dominant side: ").append(w.side()).append(")\n");
            user.append(w.text()).append('\n');
        }

        return LlmRequest.of(SYSTEM_PROMPT, user.toString());
    }

    /**
     * Chapter-level (reduce) prompt. Sees a compact digest of every scanned file — the
     * exported/public API changes produced by the map pass, and per-file risk titles —
     * enabling cross-file contract-mismatch reasoning without raw diff content.
     */
    public LlmRequest buildChapterReduce(ChapterContext context, List<ChapterFileDigest> digests,
                                         int maxContextChars) {
        StringBuilder user = new StringBuilder();

        appendContext(user, context, null, maxContextChars);

        user.append("=== Files changed together in this chapter ===\n\n");
        for (ChapterFileDigest d : digests) {
            user.append("File: ").append(d.filename())
                    .append(" (").append(d.status()).append(")\n");
            user.append("Public/exported changes:\n");
            appendChangeSummary(user, d.changeSummary());
            user.append("Per-file risks already found:");
            if (d.existingRiskTitles().isEmpty()) {
                user.append(" none\n");
            } else {
                user.append('\n');
                for (String t : d.existingRiskTitles()) {
                    user.append("  - ").append(t).append('\n');
                }
            }
            user.append('\n');
        }

        return LlmRequest.of(CROSS_FILE_SYSTEM_PROMPT, user.toString());
    }

    private void appendChangeSummary(StringBuilder user, FileChangeSummary summary) {
        if (summary == null || summary.isEmpty()) {
            user.append("  (none)\n");
            return;
        }
        appendSymbolList(user, "Added", summary.added());
        appendSymbolList(user, "Removed", summary.removed());
        appendSymbolList(user, "Modified", summary.modified());
    }

    private void appendSymbolList(StringBuilder user, String label, List<String> items) {
        if (items == null || items.isEmpty()) return;
        user.append("  ").append(label).append(":\n");
        for (String item : items) {
            user.append("    - ").append(item).append('\n');
        }
    }

    private void appendContext(StringBuilder user, ChapterContext context, String fileUnderReview,
                               int maxContextChars) {
        if (context == null) return;

        user.append("=== Chapter context ===\n");
        if (notBlank(context.walkthroughTitle())) {
            user.append("Walkthrough: ").append(context.walkthroughTitle()).append('\n');
        }
        if (notBlank(context.walkthroughDescription())) {
            user.append("Walkthrough description: ")
                    .append(truncate(context.walkthroughDescription(), maxContextChars)).append('\n');
        }
        if (notBlank(context.chapterTitle())) {
            user.append("Chapter: ").append(context.chapterTitle()).append('\n');
        }
        if (notBlank(context.chapterDescription())) {
            user.append("Chapter description: ")
                    .append(truncate(context.chapterDescription(), maxContextChars)).append('\n');
        }
        if (context.siblingFiles() != null && !context.siblingFiles().isEmpty()) {
            user.append("Files changed together in this chapter:\n");
            for (ChapterContext.FileRef f : context.siblingFiles()) {
                user.append("  - ").append(f.filename())
                        .append(" (").append(f.status()).append(')');
                if (fileUnderReview != null && fileUnderReview.equals(f.filename())) {
                    user.append("  <-- file under review");
                }
                user.append('\n');
            }
        }
        user.append('\n');
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String truncate(String s, int max) {
        if (max <= 0 || s.length() <= max) return s;
        return s.substring(0, max) + "…[truncated]";
    }

    /**
     * Compact per-file summary fed to the reduce pass: the exported/public API changes the
     * map LLM identified (semantics), plus the titles of risks already detected. No raw diff
     * content is included — the reduce pass reasons purely over the public contract surface.
     */
    public record ChapterFileDigest(
            String filename,
            String status,
            FileChangeSummary changeSummary,
            List<String> existingRiskTitles
    ) {}
}
