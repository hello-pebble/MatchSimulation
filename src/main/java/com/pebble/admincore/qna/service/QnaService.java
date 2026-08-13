package com.pebble.admincore.qna.service;

import com.pebble.admincore.common.ApiException;
import com.pebble.admincore.common.PageRequests;
import com.pebble.admincore.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.pebble.admincore.qna.domain.Qna;
import com.pebble.admincore.user.domain.User;
import com.pebble.admincore.qna.domain.QnaStatus;
import com.pebble.admincore.qna.dto.QnaDtos.QnaResponse;
import com.pebble.admincore.qna.repository.QnaRepository;
import com.pebble.admincore.user.repository.UserRepository;
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
