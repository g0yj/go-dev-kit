package com.app.api.payment;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum PaymentType {
    INCOME("수익"),
    EXPENSE("지출"),
    REFUND("환불")
    ;
    String label;
}