package com.crowdsiege.api.model.converter;

import com.crowdsiege.api.model.CodedEnum;
import com.crowdsiege.api.model.PlayerKind;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PlayerKindConverter implements AttributeConverter<PlayerKind, Short> {

    @Override
    public Short convertToDatabaseColumn(PlayerKind attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public PlayerKind convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : CodedEnum.fromCode(PlayerKind.class, dbData);
    }
}
