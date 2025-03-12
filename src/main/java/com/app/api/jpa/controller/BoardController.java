package com.app.api.jpa.controller;

import com.app.api.jpa.controller.dto.CreateNoticeRequest;
import com.app.api.jpa.controller.dto.CreateNoticeResponse;
import com.app.api.jpa.service.BoardService;
import com.app.api.jpa.service.dto.CreateNotice;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;
    private final BoardControllerMapper boardControllerMapper;

    @PostMapping("/notice")
    public CreateNoticeResponse createdNotice(@RequestHeader("Authorization") String authHeader, CreateNoticeRequest request){
        CreateNotice createNotice = boardControllerMapper.toCreateNotice(authHeader, request);

        return boardService.createdNotice(createNotice);
    }

}
