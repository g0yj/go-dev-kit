package com.app.api.file.dto;

import com.app.api.file.FileCondition;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@SuperBuilder
@Getter
public class ExcelCondition extends FileCondition {
    @Builder.Default
    String sheetName = "Sheet1"; // ✅ 기본 시트명 설정

    boolean hasHeader;

    //✅ DB 저장 시 필드명이 일치하지 않을 경우 사용.(컬럼 매핑 정보 (예: {"이름": 0, "나이": 1}))
    Map<String, String> fieldMappings;

    // ✅  이외 다양한 조건을 추가해 filter 할 수 있음.
    int startRow;
    String sortColumn; // 정렬의 기준이 될 컬럼
    boolean keepBlankLines; // 공백 필터링 여부 (DB 저장 시 불 필요한 데이터 제거 가능)


}
