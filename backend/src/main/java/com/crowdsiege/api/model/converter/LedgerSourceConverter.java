package com.crowdsiege.api.model.converter;

import com.crowdsiege.api.model.CodedEnum;
import com.crowdsiege.api.model.LedgerSource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LedgerSourceConverter implements AttributeConverter<LedgerSource, Short> {

    @Override
    public Short convertToDatabaseColumn(LedgerSource attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public LedgerSource convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : CodedEnum.fromCode(LedgerSource.class, dbData);
    }
}
