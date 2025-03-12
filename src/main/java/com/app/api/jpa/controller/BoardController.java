package com.app.api.jpa.controller;

import com.app.api.jpa.controller.dto.CreateNoticeRequest;
import com.app.api.jpa.controller.dto.CreateNoticeResponse;
import com.app.api.jpa.controller.dto.ListNoticeRequest;
import com.app.api.jpa.controller.dto.ListNoticeResponse;
import com.app.api.jpa.dto.PageResponse;
import com.app.api.jpa.service.BoardService;
import com.app.api.jpa.service.dto.CreateNotice;
import com.app.api.jpa.service.dto.SearchListNotice;
import com.app.api.jpa.service.dto.SearchNoticeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;
    private final BoardControllerMapper boardControllerMapper;

    @GetMapping("/notice/page")
    public PageResponse<ListNoticeResponse> pageListNotice(ListNoticeRequest request){
        SearchListNotice searchListNotice = boardControllerMapper.toSearchListNotice(request);
        Page<SearchNoticeResponse> noticePage = boardService.pageListNotice(searchListNotice);
        return boardControllerMapper.toListNoticeResponse(noticePage, searchListNotice);
    }
    @PostMapping("/notice")
    public CreateNoticeResponse createdNotice(@RequestHeader("Authorization") String authHeader, CreateNoticeRequest request){
        CreateNotice createNotice = boardControllerMapper.toCreateNotice(authHeader, request);

        return boardService.createdNotice(createNotice);
    }

}
