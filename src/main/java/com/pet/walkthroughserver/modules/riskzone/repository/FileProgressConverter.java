package com.pet.walkthroughserver.modules.riskzone.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Converter
public class FileProgressConverter implements AttributeConverter<List<FileProgressEntry>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<FileProgressEntry>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<FileProgressEntry> entries) {
        if (entries == null) return null;
        try {
            return MAPPER.writeValueAsString(entries);
        } catch (Exception e) {
            log.error("Failed to serialize file_progress", e);
            return "[]";
        }
    }

    @Override
    public List<FileProgressEntry> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return MAPPER.readValue(json, TYPE);
        } catch (Exception e) {
            log.error("Failed to deserialize file_progress", e);
            return new ArrayList<>();
        }
    }
}
