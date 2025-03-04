package com.app.api.file.service;

import com.app.api.file.FileCondition;
import com.app.api.file.FileProcessor;
import com.app.api.file.FileService;
import com.app.api.file.dto.CsvCondition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class CsvProcessor implements FileProcessor<CsvCondition> {

    @Override
    public List<List<String>> readFile(File file, CsvCondition condition) {
        List<List<String>> rawData = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int row = 0;
            while ((line = reader.readLine()) != null) {
                if (row++ < condition.getStartRow()) continue; // ✅ 특정 줄부터 읽기
                rawData.add(Arrays.asList(line.split(",")));
            }
        } catch (IOException e) {
            log.error("❌ [CSV 읽기 오류]: {}", e.getMessage());
        }
        return rawData;
    }

    @Override
    public List<Map<String, String>> convertData(List<List<String>> rawData, CsvCondition condition) {
        List<Map<String, String>> convertedData = new ArrayList<>();
        Map<String, String> fieldMappings = condition.getFieldMappings();

        for (List<String> row : rawData) {
            Map<String, String> rowMap = new HashMap<>();
            int index = 0;
            for (String value : row) {
                String key = fieldMappings.getOrDefault("column_" + (index + 1), "column_" + (index + 1));
                rowMap.put(key, value);
                index++;
            }
            convertedData.add(rowMap);
        }
        return convertedData;
    }
    @Override
    public String saveFile(File file, String savePath) {
        return null;
    }

    @Override
    public FileService.FileType detectFileType(File file) {
        return null;
    }

    @Override
    public String convertFileFormat(File file, String targetFormat) {
        return null;
    }

    @Override
    public List<Map<String, String>> filterData(List<Map<String, String>> data, List<FileCondition> filters) {
        return null;
    }

    @Override
    public boolean deleteFile(File file) {
        return false;
    }

    @Override
    public boolean validateFile(File file, CsvCondition condition) {
        return false;
    }
}
