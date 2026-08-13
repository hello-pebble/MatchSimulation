package com.pebble.admincore.qna.repository;

import com.pebble.admincore.qna.domain.Qna;
import com.pebble.admincore.qna.domain.QnaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QnaRepository extends JpaRepository<Qna, Long> {
    Page<Qna> findByStatus(QnaStatus status, Pageable pageable);
}
