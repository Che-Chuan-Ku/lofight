package com.crowdsiege.api.model.converter;

import com.crowdsiege.api.model.CodedEnum;
import com.crowdsiege.api.model.DeathCause;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DeathCauseConverter implements AttributeConverter<DeathCause, Short> {

    @Override
    public Short convertToDatabaseColumn(DeathCause attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public DeathCause convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : CodedEnum.fromCode(DeathCause.class, dbData);
    }
}
