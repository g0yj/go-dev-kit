package com.app.api.batch;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum BatchCode {
    CONSULTATION_DB("상담내역DB관리"),
    PAYMENT_BATCH("월별매출데이터관리")

    ;
    String label;

}
