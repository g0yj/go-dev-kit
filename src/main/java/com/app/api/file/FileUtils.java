package com.app.api.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
@Slf4j
@Component
public class FileUtils {

    private static final String PROCESSED_FILE_RECORD = "processedFiles.txt"; // 중복 다운로드 방지용 파일 기록 -> 다운로드 된 파일을 processedFiles.txt 문서에 저장
    private final Set<String> processedFiles = new HashSet<>(); // 중복 다운로드 방지를 위해 사용되는 파일 목록 저장(이미 처리된 파일을 저장하여 중복 다운로드를 방지)

    /**
     * 📌 파일 다운로드 후 처리 완료된 파일을 기록
     */
    public void markFileAsProcessed(String fileName) {
        processedFiles.add(fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PROCESSED_FILE_RECORD, true))) {
            writer.write(fileName + "\n");
        } catch (IOException e) {
            log.error("❌ [파일 처리 기록 실패] {}", fileName, e);
        }
    }

    /**
     * 📌 기존 처리된 파일 기록 불러오기
     */
    public void loadProcessedFiles() {
        File file = new File(PROCESSED_FILE_RECORD);
        if (!file.exists()) return; // 기록 파일이 없으면 로딩하지 않음

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processedFiles.add(line.trim()); // 저장된 파일명을 HashSet에 추가
            }
        } catch (IOException e) {
            log.error("❌ [처리된 파일 로드 실패]", e);
        }
    }

    public boolean deleteFile(File file) {
        return file.exists() && file.delete();
    }



    /**
     * 📌 파일명 정리 (특수문자 제거)
     */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unknown_file"; // 기본 파일명
        }

        // ✅ OS에서 허용되지 않는 문자 제거
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        // ✅ 파일명이 너무 길 경우, 앞부분을 잘라내고 확장자 유지
        int maxLength = 100;
        if (fileName.length() > maxLength) {
            int lastDotIndex = fileName.lastIndexOf(".");
            String extension = (lastDotIndex > 0) ? fileName.substring(lastDotIndex) : "";
            fileName = fileName.substring(0, maxLength - extension.length()) + extension;
        }

        return fileName.trim();
    }

    /**
     * 📌 파일 존재 여부 확인
     */
    public static boolean isFileExists(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.isFile();
    }



}
