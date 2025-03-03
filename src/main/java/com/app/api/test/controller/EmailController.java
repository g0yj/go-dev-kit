package com.app.api.test.controller;

import com.app.api.test.controller.dto.email.EmailResponse;
import com.app.api.test.controller.dto.email.SearchlRequestEmail;
import com.app.api.email.EmailReceiverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 📂 이메일 관련 컨트롤러
 */
@Tag( name="이메일 관리 API" , description = "이메일 관련 기능을 제공합니다.")
@Slf4j
@RestController
@RequestMapping("/email")
@RequiredArgsConstructor

public class EmailController {
    private final EmailReceiverService emailReceiverService;
    /**
     * 📌 이메일 조회
     *    @param email 필터링 조건 (발신자, 수신자, 날짜 등)
     *    @return 필터링 된 이메일 목록
     */
    @GetMapping
    @Operation(summary ="필터링 된 이메일 조회")
    List<EmailResponse> getList(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "핉터링 조건") @RequestBody SearchlRequestEmail email){
        log.debug("✅ [이메일 전체 조회 Cotroller ] email : {} ", email);
        return emailReceiverService.getList(email);
    }


}
