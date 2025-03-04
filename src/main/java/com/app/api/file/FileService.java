package com.app.api.file;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 📂 공통 파일 처리 서비스
 */
@Component
@Getter@Setter@AllArgsConstructor
@Slf4j
@RequiredArgsConstructor
public class FileService {
    @Value("${app.file.host}")
    private String host;

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @Value("${app.file.max-file-size}")
    private String maxFileSizeStr;


    public Map<String, String> upload(List<MultipartFile> files) {
        Map<String, String> fileNames = new HashMap<>();

        if (files == null) return fileNames;

        files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .forEach(file -> {
                    if (file.getSize() > getMaxFileSize()) {
                        throw new IllegalArgumentException(
                                String.format("❌ 파일 크기가 허용된 크기(%d MB)를 초과했습니다: %s", maxFileSizeStr, file.getOriginalFilename()));
                    }
                    String fileName = upload(file);

                    if (fileName != null) fileNames.put(file.getOriginalFilename(), fileName);
                });

        return fileNames;
    }

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 파일 크기 체크
        if (file.getSize() > getMaxFileSize()) {
            throw new IllegalArgumentException(
                    String.format("❌ 파일 크기가 허용된 크기(%d MB)를 초과: %s", maxFileSizeStr, file.getOriginalFilename()));
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        File dir = new File(uploadDir);
        if(!dir.exists()) {
            if(!dir.mkdirs()) {
                throw new IllegalArgumentException("❌ 파일을 업로드 할 폴더 생성에 실패");
            }
        }
        try {
            file.transferTo(new File(uploadDir +fileName));
            return fileName;
        } catch (IOException e){
            throw new IllegalArgumentException("❌ [업로드 실패]" , e);
        }

    }
    /**
     * 📌 URL에서 파일 다운로드
     */
    public File downloadFromUrl(String fileUrl, String saveDir) {
        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // ✅ 파일명 가져오기
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            File file = new File(saveDir + "/" + fileName);

            // ✅ InputStream으로 읽고 파일 저장
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            log.info("✅ [파일 다운로드 완료] {} → {}", fileUrl, file.getAbsolutePath());
            return file;
        } catch (Exception e) {
            log.error("❌ [파일 다운로드 실패] URL: {}, 오류: {}", fileUrl, e.getMessage());
            return null;
        }
    }

    /**
     * 📌 바이트 데이터로부터 파일 다운로드
     */
    public File downloadFromBytes(byte[] fileBytes, String fileName, String saveDir) {
        try {
            if (fileBytes == null || fileBytes.length == 0) {
                log.warn("⚠️ [파일 저장 실패] 파일 데이터가 존재하지 않습니다.");
                return null;
            }

            String sanitizedFileName = sanitizeFileName(fileName);
            File file = new File(saveDir + "/" + sanitizedFileName);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(fileBytes);
            }

            log.info("✅ [첨부파일 저장 완료] {}", file.getAbsolutePath());
            return file;
        } catch (Exception e) {
            log.error("❌ [파일 저장 실패] 파일명: {}, 오류: {}", fileName, e.getMessage());
            return null;
        }
    }

    /**
     * 📌 파일명에서 불필요한 문자 제거
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unknown_file";
        }
        // OS에서 허용되지 않는 문자 제거
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        // 확장자 유지하면서 100자 이내로 자르기
        int maxLength = 100;
        int lastDotIndex = fileName.lastIndexOf(".");
        String extension = (lastDotIndex > 0) ? fileName.substring(lastDotIndex) : "";
        if (fileName.length() > maxLength) {
            fileName = fileName.substring(0, maxLength - extension.length()) + extension;
        }

        return fileName.trim();
    }


    /**
     * 📌 파일 다운로드 URL 생성
     */
    public String getUrl(String fileName, String originalFileName){
        if(fileName == null || originalFileName == null){
            return null;
        }
        return host + "/file/download" + fileName + "/" + originalFileName;
    }

    /**
     * 📌 확장자 관리
     */
    public enum FileType {
        CSV, EXCEL, PDF, DOCX, TXT, UNKNOWN;

        public static FileType fromExtension(File file) {
            String fileName = file.getName();
            int dotIndex = fileName.lastIndexOf('.');

            // 확장자가 없거나 점 뒤에 문자가 없는 경우
            if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
                return UNKNOWN;
            }

            // 확장자 추출
            String extension = fileName.substring(dotIndex + 1).toLowerCase();

            // 확장자에 따른 파일 타입 반환
            switch (extension) {
                case "csv":
                    return CSV;
                case "xlsx":
                    return EXCEL;
                case "pdf":
                    return PDF;
                case "docx":
                    return DOCX;
                case "txt":
                    return TXT;
                default:
                    return UNKNOWN;
            }
        }
    }
    private long getMaxFileSize() {
        return DataSize.parse(maxFileSizeStr).toBytes(); // 파일 크기 문자열을 바이트 단위로 변환
    }

    /**
     * 📌 주어진 경로에 디렉토리가 존재하지 않으면 생성
     * @param directoryPath 생성할 디렉토리 경로
     */
    public void createDirectoryIfNotExists(String directoryPath) {
        File directory = new File(directoryPath);

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                log.info("✅ [디렉토리 생성] 경로: {}", directoryPath);
            } else {
                log.error("❌ [디렉토리 생성 실패] 경로: {}", directoryPath);
                throw new RuntimeException("디렉토리 생성 실패: " + directoryPath);
            }
        }
    }
}

/**
 * 멀티파트 방식 (MultipartFile): 처음에는 MultipartFile 타입으로 받지만, 최종적으로 File로 변환하여 저장
 * Base64 방식 (String): Base64 인코딩된 String으로 전송되고, 디코딩 후 File로 변환하여 저장
 */