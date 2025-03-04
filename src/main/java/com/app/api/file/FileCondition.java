package com.app.api.file;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;


@SuperBuilder // 부모 클래스에서 빌더 패턴을 지원
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PROTECTED)
public abstract class FileCondition {

    String filePath ;

    @Builder.Default
    String delimiter = ",";

    @Builder.Default
    String encoding = "UTF-8";

    @Builder.Default
    String sortOrder = "ASC";

    @Builder.Default
    String pageSize = "A4";

    @Value("${app.file.upload-dir}")
    String savePath;

    @Builder.Default
    int startRow = 1;

    boolean hasHeader;

    boolean keepBlankLines; // 공백 필터링 여부 (DB 저장 시 불 필요한 데이터 제거 가능)

    FileService.FileType fileType;

    Map<String, String> fieldMappings; //DB 저장 시 필드명이 일치하지 않을 경우 사용.
}

