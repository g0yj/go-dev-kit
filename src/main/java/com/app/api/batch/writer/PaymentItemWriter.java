package com.app.api.batch.writer;

import com.app.api.batch.BatchCode;
import com.app.api.batch.BatchJobLogService;
import com.app.api.batch.BatchStatus;
import com.app.api.batch.processor.PaymentDTO;
import com.app.api.email.EmailConfig;
import com.app.api.email.EmailSenderService;
import com.app.api.email.dto.SendEmailRequest;
import com.app.api.file.FileService;
import com.app.api.file.processor.ExcelProcessor;
import com.app.api.jpa.entity.BatchJobLogEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 📌 결제 데이터를 엑셀 파일로 저장하고 이메일로 전송하는 ItemWriter
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentItemWriter implements ItemWriter<PaymentDTO>, StepExecutionListener {
    private final ExcelProcessor excelProcessor;
    private final EmailSenderService emailSenderService;
    private final EmailConfig emailConfig;
    private final FileService fileService;
    private final BatchJobLogService batchJobLogService;

    private final List<PaymentDTO> allPayments = new ArrayList<>();
    private String savedFilePath;

    private Long batchLogId; // ✅ 배치 로그 ID 저장

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("🚀 [PaymentItemWriter] 데이터 수집을 준비합니다.");
        BatchJobLogEntity batchJobLog = batchJobLogService.startBatchLog(BatchCode.PAYMENT_BATCH);
        this.batchLogId = batchJobLog.getId();
    }

    @Override
    public void write(Chunk<? extends PaymentDTO> chunk) {
        try {
            log.info("📌 [PaymentItemWriter] 데이터 수집 중... (Chunk 크기: {})", chunk.getItems().size());

            allPayments.addAll(chunk.getItems());

        } catch (Exception e) {
            batchJobLogService.updateBatchLogMessage(batchLogId, "❌ [데이터 수집 오류] " + e.getMessage());
            throw e;
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("✅ [PaymentItemWriter] 모든 데이터 수집 완료! 엑셀 파일을 생성합니다...");

        if (allPayments.isEmpty()) {
            batchJobLogService.updateBatchLogMessage(batchLogId, "처리할 데이터 없음");
            log.warn("⚠️ [PaymentItemWriter] 저장할 데이터가 없습니다.");
            return ExitStatus.FAILED;
        }

        // ✅ 파일명 생성 (고유한 파일명 유지)
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "payments_" + timestamp + ".xlsx";
        Path savePath = Paths.get(fileService.getUploadDir(), fileName);

        // ✅ 엑셀 파일 생성
        try {
            List<String> headers = List.of("ID", "회사명", "사용자명", "결제유형", "결제금액", "미수금", "설명", "결제일");
            List<List<String>> dataList = convertToDataList(allPayments);

            savedFilePath = excelProcessor.createExcelFile(headers, dataList, savePath.toString());
            if (savedFilePath == null) {
                String errorMessage = "❌ [엑셀 파일 생성 오류] 파일을 저장할 경로가 존재하지 않습니다";
                batchJobLogService.updateBatchLogMessage(batchLogId, errorMessage);
                return ExitStatus.FAILED;
            }
        } catch (Exception e) {
            String errorMessage = "❌ [엑셀 파일 생성 오류] " + e.getMessage();
            batchJobLogService.updateBatchLogMessage(batchLogId, errorMessage);
            log.error(errorMessage, e);
            return ExitStatus.FAILED; // ❌ 파일 저장 실패 시 배치 실패 처리
        }

        // ✅ 엑셀 파일이 생성되지 않았다면 메일 전송을 건너뛰기
        if (savedFilePath == null) {
            log.warn("⚠️ [PaymentItemWriter] 파일이 존재하지 않아 메일 전송을 건너뜁니다.");
            return ExitStatus.FAILED;
        }

        // ✅ 메일 전송
        try {
            sendEmailWithAttachment(new File(savedFilePath));
        } catch (Exception e) {
            String errorMessage = "❌ [메일 전송 오류] " + e.getMessage();
            batchJobLogService.updateBatchLogMessage(batchLogId, errorMessage);
            log.error(errorMessage, e);
            return ExitStatus.FAILED; // ❌ 메일 전송 실패 시 배치 실패 처리
        }

        // ✅ 배치 완료 로그 저장
        batchJobLogService.completeBatchLog(batchLogId, 0, 0, 0, BatchStatus.COMPLETED, "배치 성공적으로 완료됨");
        return ExitStatus.COMPLETED;
    }

    /**
     * 📌 `PaymentDTO` 데이터를 `List<List<String>>` 리스트로 변환
     */
    private List<List<String>> convertToDataList(List<PaymentDTO> payments) {
        return payments.stream()
                .map(payment -> List.of(
                        String.valueOf(payment.getId()),
                        payment.getCompanyName() != null ? payment.getCompanyName() : "",
                        payment.getUserName() != null ? payment.getUserName() : "",
                        payment.getPaymentType() != null ? payment.getPaymentType().name() : "",
                        String.valueOf(payment.getAmount()),
                        String.valueOf(payment.getOutstandingAmount()),
                        payment.getDescription() != null ? payment.getDescription() : "",
                        payment.getPayDate() != null ? payment.getPayDate().toString() : ""
                ))
                .collect(Collectors.toList());
    }

    /**
     * 📌 이메일 전송 메서드
     */
    private void sendEmailWithAttachment(File file) {
        SendEmailRequest emailRequest = SendEmailRequest.builder()
                .toEmail(emailConfig.getEmailProperties().getUsername())
                .subject("📑 결제 보고서")
                .body("<h3>📌 결제 데이터 보고서 첨부</h3><p>엑셀 파일을 확인해주세요.</p>")
                .attachments(List.of(file))
                .build();
        emailSenderService.sendEmailWithAttachment(emailRequest);
    }
}
