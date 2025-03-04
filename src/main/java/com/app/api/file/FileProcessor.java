package com.app.api.file;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 📌 파일 처리 인터페이스 (파일 읽기, 변환 및 데이터 처리)
 */
public interface FileProcessor<T extends FileCondition> {

    /**
     * 📌 파일을 읽고 원본 데이터를 반환
     * - 이 메서드는 데이터를 가공하지 않고 원본 그대로 반환
     * - CSV: 각 행을 배열(String[])로 반환
     * - PDF: 각 문장을 리스트(String)로 반환
     * - Excel: 셀 데이터를 리스트(String)로 반환
     * @param file 처리할 파일 객체
     * @param condition 파일 처리 조건
     * @return 원본 데이터 리스트
     */
    List<List<String>> readFile(File file, T condition);

    /**
     * 📌 원본 데이터를 Key-Value 형태로 변환
     * - 데이터를 특정 형식 (ex: DB 저장용)으로 가공
     * - CSV: 헤더가 있는 경우 컬럼명을 Key로 매핑
     * - PDF: 특정 패턴에 따라 Key-Value 변환
     * - Excel: 특정 열을 기준으로 Key-Value 변환
     * @param rawData 원본 데이터 리스트 (readFile 메서드에서 반환된 데이터)
     * @param condition 변환 조건
     * @return 변환된 데이터 리스트 (Key-Value 형태)
     */
    List<Map<String, String>> convertData(List<List<String>> rawData, T condition);

    /**
     * 📌 파일을 저장 (예: 로컬, S3 등)
     * - 파일을 특정 경로 또는 클라우드 저장소에 저장하는 기능
     * @param file 저장할 파일 객체
     * @param savePath 저장할 경로
     * @return 저장된 파일의 경로
     */
    String saveFile(File file, String savePath);

    /**
     * 📌 파일 유형 감지 (확장자 기반)
     * - 파일 확장자를 분석하여 파일 유형을 반환
     * @param file 검사할 파일 객체
     * @return 파일 유형 (`FileType`)
     */
    FileService.FileType detectFileType(File file);

    /**
     * 📌 파일 포맷 변환 (ex: CSV → JSON, PDF → TXT)
     * - 파일을 다른 형식으로 변환하는 기능
     * @param file 변환할 파일 객체
     * @param targetFormat 변환할 형식 (ex: "json", "txt")
     * @return 변환된 파일 경로 또는 변환된 데이터
     */
    String convertFileFormat(File file, String targetFormat);

    /**
     * 📌 데이터 필터링 (ex: 특정 컬럼 값 기준으로 필터링)
     * - 데이터에서 특정 조건을 만족하는 항목만 필터링
     * @param data 원본 데이터 리스트 (Key-Value 형태)
     * @param filters 적용할 필터 조건 리스트
     * @return 필터링된 데이터 리스트
     */
    List<Map<String, String>> filterData(List<Map<String, String>> data, List<FileCondition> filters);

    /**
     * 📌 파일 삭제 (ex: 임시 파일 삭제)
     * - 특정 파일을 삭제하는 기능
     * @param file 삭제할 파일 객체
     * @return 삭제 성공 여부 (`true`: 삭제 성공, `false`: 삭제 실패)
     */
    boolean deleteFile(File file);

    /**
     * 📌 파일 유효성 검사 (ex: 특정 필드 존재 여부, 데이터 손상 확인)
     * - 파일이 정상적으로 읽을 수 있는지 검사
     * @param file 검사할 파일 객체
     * @param condition 파일 처리 조건
     * @return 유효성 검사 결과 (`true`: 유효한 파일, `false`: 유효하지 않은 파일)
     */
    boolean validateFile(File file, T condition);
}
