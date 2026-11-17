package com.crowdsiege.api.model.converter;

import com.crowdsiege.api.model.CodedEnum;
import com.crowdsiege.api.model.PaymentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, Short> {

    @Override
    public Short convertToDatabaseColumn(PaymentStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public PaymentStatus convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : CodedEnum.fromCode(PaymentStatus.class, dbData);
    }
}
