package com.crowdsiege.api.model.converter;

import com.crowdsiege.api.model.CodedEnum;
import com.crowdsiege.api.model.RunState;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RunStateConverter implements AttributeConverter<RunState, Short> {

    @Override
    public Short convertToDatabaseColumn(RunState attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public RunState convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : CodedEnum.fromCode(RunState.class, dbData);
    }
}
