package com.pet.walkthroughserver.modules.comment.repository;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link SyncStatus} to its lowercase column value so the database keeps storing
 * {@code "pending"/"synced"/"failed"/"permanently_failed"} exactly as before.
 */
@Converter
public class SyncStatusConverter implements AttributeConverter<SyncStatus, String> {

    @Override
    public String convertToDatabaseColumn(SyncStatus attribute) {
        return attribute == null ? null : attribute.dbValue();
    }

    @Override
    public SyncStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SyncStatus.fromDbValue(dbData);
    }
}
