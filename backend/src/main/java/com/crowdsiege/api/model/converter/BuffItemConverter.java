package com.crowdsiege.api.model.converter;

import com.crowdsiege.api.model.CodedEnum;
import com.crowdsiege.api.model.BuffItem;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BuffItemConverter implements AttributeConverter<BuffItem, Short> {

    @Override
    public Short convertToDatabaseColumn(BuffItem attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public BuffItem convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : CodedEnum.fromCode(BuffItem.class, dbData);
    }
}
