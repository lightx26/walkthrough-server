package com.pet.walkthroughserver.modules.riskzone.repository;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RiskCategory {
    RACE_CONDITION("Race Condition"),
    BREAKING_CHANGE("Breaking Change"),
    MISSING_ROLLBACK("Missing Rollback"),
    DATA_INTEGRITY("Data Integrity"),
    INPUT_VALIDATION("Input Validation"),
    AUTH_AUTHZ("Auth / Authz"),
    ERROR_HANDLING("Error Handling"),
    BUSINESS_LOGIC("Business Logic"),
    REMOVED_TESTS("Removed Tests"),
    OTHER("Other");

    private final String label;

    RiskCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static RiskCategory fromString(String value) {
        if (value == null) return OTHER;
        for (RiskCategory c : values()) {
            if (c.name().equalsIgnoreCase(value.trim())) return c;
        }
        return OTHER;
    }
}
