package com.pebble.mvp.repository;

import com.pebble.mvp.domain.Qna;
import com.pebble.mvp.domain.enums.QnaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QnaRepository extends JpaRepository<Qna, Long> {
    List<Qna> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Qna> findByStatusOrderByCreatedAtDesc(QnaStatus status);
    List<Qna> findAllByOrderByCreatedAtDesc();
}
