package com.pet.walkthroughserver.modules.riskzone.repository;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FileProgressEntry(
        @JsonProperty("filename") String filename,
        @JsonProperty("status")   String status
) {}
