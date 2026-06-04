package com.pet.walkthroughserver.modules.riskzone.business.prompt;

import com.pet.walkthroughserver.modules._shared.infra.ai.LlmRequest;
import com.pet.walkthroughserver.modules._shared.util.UnifiedDiff.DiffWindow;
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

            Respond with a JSON array of risk objects. Each object must have:
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

            Return an empty array [] if no risks are found. Do not include markdown fences.
            Only return the JSON array, nothing else.
            """;

    public LlmRequest build(String filename, String fileStatus, List<DiffWindow> windows) {
        StringBuilder user = new StringBuilder();
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
}
