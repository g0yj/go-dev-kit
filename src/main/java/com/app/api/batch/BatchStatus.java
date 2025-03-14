package com.app.api.batch;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum BatchStatus {
    STARTED("배치 실행 시작"),
    COMPLETED("배치 실행 완료"),
    FAILED("배치 실행 실패"),
    RUNNING("배치 실행 중"),
    STOPPED("배치 중단");
    ;

    String label;

}
