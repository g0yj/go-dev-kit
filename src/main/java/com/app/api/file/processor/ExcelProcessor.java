package com.app.api.file.processor;

import com.app.api.file.FileProcessor;
import com.app.api.file.dto.ExcelCondition;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
@Slf4j
public class ExcelProcessor implements FileProcessor<ExcelCondition> {
    /**
     * 📌 Excel 파일을 읽고 원본 데이터를 반환
     * - 데이터를 가공하지 않고 원본 그대로 반환
     */
    @Override
    public List<List<String>> readFile(File file) {
        List<List<String>> rawData = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // ✅ 첫 번째 시트 가져오기
            if (sheet == null) {
                log.error("❌ [ExcelProcessor] 시트를 찾을 수 없습니다.");
                return rawData;
            }

            for (Row row : sheet) {
                List<String> rowData = new ArrayList<>();
                for (Cell cell : row) {
                    rowData.add(getCellValue(cell));
                }
                rawData.add(rowData);
            }

            log.info("✅ [ExcelProcessor] 총 {}개의 행을 읽었습니다.", rawData.size());
        } catch (IOException e) {
            log.error("❌ [ExcelProcessor] Excel 파일 읽기 오류: {}", e.getMessage());
        }

        return rawData;
    }

    /**
     * 📌 Excel 데이터를 변환 (Key-Value 형식으로 변환)
     * - hasHeader가 true이면 첫 번째 행을 헤더로 사용
     * - hasHeader가 false이면 column_1, column_2 형식으로 헤더를 자동 생성
     * - fieldMappings이 존재하면, 컬럼명을 매핑된 값으로 변경
     * - 필터 조건을 적용하여 필요한 데이터만 남김
     */
    public List<Map<String, String>> convertData(List<List<String>> rawData, ExcelCondition condition) {
        List<Map<String, String>> convertedData = new ArrayList<>();

        if (rawData.isEmpty()) {
            log.error("❌ [ExcelProcessor] 변환할 데이터가 없습니다.");
            return convertedData;
        }

        List<String> header;

        if (condition.isHasHeader()) {
            header = rawData.get(0); // ✅ 첫 번째 행을 헤더로 사용
        } else {
            int columnSize = rawData.get(0).size();
            header = new ArrayList<>();
            for (int i = 0; i < columnSize; i++) {
                header.add("column_" + (i + 1));
            }
        }

        // ✅ 필드 매핑이 존재하면, 매핑된 값으로 컬럼명을 변경
        Map<String, String> fieldMappings = condition.getFieldMappings();

        for (int i = 0; i < header.size(); i++) {
            String originalColumn = header.get(i);
            if (fieldMappings.containsKey(originalColumn)) {
                header.set(i, fieldMappings.get(originalColumn)); // ✅ 매핑된 컬럼명 적용
            }
        }

        // ✅ 데이터 변환 후 필터링 적용
        int startRowIndex = condition.getStartRow();

        for (int i = startRowIndex; i < rawData.size(); i++) {
            List<String> row = rawData.get(i);
            Map<String, String> rowMap = new HashMap<>();

            for (int j = 0; j < header.size(); j++) {
                String columnName = header.get(j);
                if (j < row.size()) {
                    rowMap.put(columnName, row.get(j));
                }
            }

            // ✅ 필터 조건을 적용하여 유효한 데이터만 저장
            if (isValidRow(rowMap, condition)) {
                convertedData.add(rowMap);
            }
        }

        // ✅ 정렬 적용
        convertedData = applySorting(convertedData, condition);

        log.info("✅ [ExcelProcessor] 데이터 변환 및 필터링 완료 (총 {}개)", convertedData.size());
        return convertedData;
    }

    /**
     * 📌 필터링 조건 적용
     * - 새로운 필터 조건을 쉽게 추가할 수 있도록 확장 가능
     */
    private boolean isValidRow(Map<String, String> row, ExcelCondition condition) {
        try {
            return true; // ✅ 모든 조건을 만족하면 포함

        } catch (Exception e) {
            log.error("❌ [ExcelProcessor] 필터링 메서드 오류", e);
            return false;
        }
    }

    /**
     * 📌 정렬 적용 (sortOrder에 따라 정렬)
     * - 사용자가 설정한 컬럼을 기준으로 정렬
     * - sortOrder가 "ASC"이면 오름차순, "DESC"이면 내림차순
     */
    public List<Map<String, String>> applySorting(List<Map<String, String>> data, ExcelCondition condition) {
        String sortColumn = condition.getSortColumn(); // ✅ 정렬 기준 컬럼
        String sortOrder = condition.getSortOrder();  // ✅ 정렬 순서 (ASC / DESC)

        if (sortColumn == null || !data.get(0).containsKey(sortColumn)) {
            log.warn("⚠️ [ExcelProcessor] 유효한 정렬 컬럼이 없습니다. 정렬을 건너뜁니다.");
            return data; // ✅ 정렬할 컬럼이 없으면 원본 데이터 반환
        }

        Comparator<Map<String, String>> comparator = Comparator.comparing(row -> row.get(sortColumn), Comparator.nullsLast(String::compareTo));

        if ("DESC".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed(); // ✅ 내림차순 정렬 적용
        }

        data.sort(comparator);

        log.info("✅ [ExcelProcessor] 데이터 정렬 완료: 컬럼={} / 정렬순서={}", sortColumn, sortOrder);
        return data;
    }

    /**
     * 📌 엑셀 파일 생성 메서드 (List<List<String>> 사용)
     * @param headers 엑셀 헤더 목록
     * @param dataList 엑셀 데이터 (List<List<String>> 형식)
     * @param filePath 저장할 파일 경로
     * @return 생성된 파일 경로
     */
    public String createExcelFile(List<String> headers, List<List<String>> dataList, String filePath) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Report");

        try {
            // ✅ 헤더 생성
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }

            // ✅ 데이터 입력
            int rowNum = 1;
            for (List<String> rowData : dataList) {
                Row row = sheet.createRow(rowNum++);
                for (int colNum = 0; colNum < rowData.size(); colNum++) {
                    row.createCell(colNum).setCellValue(rowData.get(colNum));
                }
            }

            // ✅ 파일 저장
            File file = new File(filePath);
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }

            workbook.close();
            log.info("✅ [ExcelProcessor] 엑셀 파일 생성 완료: {}", filePath);
            return filePath;
        } catch (IOException e) {
            log.error("❌ [ExcelProcessor] 엑셀 파일 저장 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 📌 Excel 데이터를 파일로 저장
     */
    @Override
    public String saveFile(File file, String savePath) {
        try {
            File destFile = new File(savePath);
            try (FileInputStream fis = new FileInputStream(file);
                 FileOutputStream fos = new FileOutputStream(destFile)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
            }

            log.info("✅ [ExcelProcessor] 파일 저장 완료: {}", savePath);
            return savePath;
        } catch (IOException e) {
            log.error("❌ [ExcelProcessor] 파일 저장 실패: {}", e.getMessage());
            return null;
        }
    }



    @Override
    public boolean deleteFile(File file) {
        return false;
    }


    /**
     * 📌 Excel 셀 데이터를 String으로 변환
     */
    private String getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}
