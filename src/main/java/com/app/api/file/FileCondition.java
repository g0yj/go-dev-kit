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
    String encoding = "UTF-8";

    @Builder.Default
    String sortOrder = "ASC";

    @Value("${app.file.upload-dir}")
    String savePath;


    FileService.FileType fileType;


}

