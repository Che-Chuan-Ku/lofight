package com.crowdsiege.api.model.converter;

import com.crowdsiege.api.model.CodedEnum;
import com.crowdsiege.api.model.LoginProvider;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LoginProviderConverter implements AttributeConverter<LoginProvider, Short> {

    @Override
    public Short convertToDatabaseColumn(LoginProvider attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public LoginProvider convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : CodedEnum.fromCode(LoginProvider.class, dbData);
    }
}
