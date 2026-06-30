package com.pet.walkthroughserver.modules.riskzone.repository;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FileProgressEntry(
        @JsonProperty("filename") String filename,
        @JsonProperty("status")   String status,
        @JsonProperty("reason")   String reason
) {
    /** Progress entry without a reason (the common case for scanned files). */
    public FileProgressEntry(String filename, String status) {
        this(filename, status, null);
    }
}
