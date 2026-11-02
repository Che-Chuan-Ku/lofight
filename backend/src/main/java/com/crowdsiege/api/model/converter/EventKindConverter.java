package com.crowdsiege.api.model.converter;

import com.crowdsiege.api.model.CodedEnum;
import com.crowdsiege.api.model.EventKind;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EventKindConverter implements AttributeConverter<EventKind, Short> {

    @Override
    public Short convertToDatabaseColumn(EventKind attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public EventKind convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : CodedEnum.fromCode(EventKind.class, dbData);
    }
}
