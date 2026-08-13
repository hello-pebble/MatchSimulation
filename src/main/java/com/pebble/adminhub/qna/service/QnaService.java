package com.pebble.adminhub.qna.service;

import com.pebble.adminhub.common.ApiException;
import com.pebble.adminhub.common.PageRequests;
import com.pebble.adminhub.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.pebble.adminhub.qna.domain.Qna;
import com.pebble.adminhub.user.domain.User;
import com.pebble.adminhub.qna.domain.QnaStatus;
import com.pebble.adminhub.qna.dto.QnaDtos.QnaResponse;
import com.pebble.adminhub.qna.repository.QnaRepository;
import com.pebble.adminhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class QnaService {

    private final QnaRepository qnaRepository;
    private final UserRepository userRepository;

    public PageResponse<QnaResponse> findAll(QnaStatus status, Pageable pageable) {
        Pageable clamped = PageRequests.clamp(pageable);
        Page<Qna> page = status == null
                ? qnaRepository.findAll(clamped)
                : qnaRepository.findByStatus(status, clamped);
        return PageResponse.of(page, this::toResponse);
    }

    @Transactional
    public QnaResponse answer(Long qnaId, String answer) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> ApiException.notFound("문의를 찾을 수 없습니다: " + qnaId));
        qna.setAnswer(answer);
        qna.setStatus(QnaStatus.ANSWERED);
        qna.setAnsweredAt(LocalDateTime.now());
        return toResponse(qna);
    }

    private QnaResponse toResponse(Qna qna) {
        String userName = userRepository.findById(qna.getUserId())
                .map(User::getName)
                .orElse("(탈퇴 회원)");
        return QnaResponse.of(qna, userName);
    }
}
