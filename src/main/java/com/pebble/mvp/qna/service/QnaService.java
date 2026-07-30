package com.pebble.mvp.qna.service;

import com.pebble.mvp.common.ApiException;
import com.pebble.mvp.common.PageRequests;
import com.pebble.mvp.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.pebble.mvp.qna.domain.Qna;
import com.pebble.mvp.user.domain.User;
import com.pebble.mvp.qna.domain.QnaStatus;
import com.pebble.mvp.qna.dto.QnaDtos.QnaCreateRequest;
import com.pebble.mvp.qna.dto.QnaDtos.QnaResponse;
import com.pebble.mvp.qna.repository.QnaRepository;
import com.pebble.mvp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QnaService {

    private final QnaRepository qnaRepository;
    private final UserRepository userRepository;

    public QnaResponse create(User me, QnaCreateRequest request) {
        Qna qna = qnaRepository.save(Qna.builder()
                .userId(me.getId())
                .title(request.title())
                .question(request.question())
                .status(QnaStatus.WAITING)
                .createdAt(LocalDateTime.now())
                .build());
        return toResponse(qna);
    }

    public List<QnaResponse> myQna(User me) {
        return qnaRepository.findByUserIdOrderByCreatedAtDesc(me.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

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
