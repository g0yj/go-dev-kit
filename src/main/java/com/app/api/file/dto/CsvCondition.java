package com.app.api.file.dto;

import com.app.api.file.FileCondition;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class CsvCondition extends FileCondition {
    // 별도의 @Builder를 추가하지 않음
}
