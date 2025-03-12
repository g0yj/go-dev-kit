package com.app.api.file.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileInfo {
    private String fileName;
    private String originalFileName;
    private String filePath;
    private String fileUrl;
}