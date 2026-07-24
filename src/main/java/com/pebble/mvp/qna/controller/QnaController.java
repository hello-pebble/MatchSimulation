package com.pebble.mvp.qna.controller;

import com.pebble.mvp.qna.dto.QnaDtos.QnaCreateRequest;
import com.pebble.mvp.qna.dto.QnaDtos.QnaResponse;
import com.pebble.mvp.user.service.AuthService;
import com.pebble.mvp.qna.service.QnaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qna")
@RequiredArgsConstructor
public class QnaController {

    private final AuthService authService;
    private final QnaService qnaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QnaResponse create(@RequestHeader("X-AUTH-TOKEN") String token,
                              @Valid @RequestBody QnaCreateRequest request) {
        return qnaService.create(authService.authenticate(token), request);
    }

    @GetMapping("/my")
    public List<QnaResponse> myQna(@RequestHeader("X-AUTH-TOKEN") String token) {
        return qnaService.myQna(authService.authenticate(token));
    }
}
