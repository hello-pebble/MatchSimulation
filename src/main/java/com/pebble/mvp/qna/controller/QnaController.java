package com.pebble.mvp.qna.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.pebble.mvp.qna.dto.QnaDtos.QnaCreateRequest;
import com.pebble.mvp.qna.dto.QnaDtos.QnaResponse;
import com.pebble.mvp.qna.service.QnaService;
import com.pebble.mvp.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "QnA", description = "1:1 문의 등록 및 조회")
@RestController
@RequestMapping("/api/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QnaResponse create(@AuthenticationPrincipal User me,
                              @Valid @RequestBody QnaCreateRequest request) {
        return qnaService.create(me, request);
    }

    @GetMapping("/my")
    public List<QnaResponse> myQna(@AuthenticationPrincipal User me) {
        return qnaService.myQna(me);
    }
}
