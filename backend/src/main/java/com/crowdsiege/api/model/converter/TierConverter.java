package com.crowdsiege.api.model.converter;

import com.crowdsiege.api.model.CodedEnum;
import com.crowdsiege.api.model.Tier;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TierConverter implements AttributeConverter<Tier, Short> {

    @Override
    public Short convertToDatabaseColumn(Tier attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public Tier convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : CodedEnum.fromCode(Tier.class, dbData);
    }
}
