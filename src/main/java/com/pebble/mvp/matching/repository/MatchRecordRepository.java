package com.pebble.mvp.matching.repository;

import com.pebble.mvp.matching.domain.MatchRecord;
import com.pebble.mvp.matching.domain.MatchStatus;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MatchRecordRepository extends JpaRepository<MatchRecord, Long> {
    List<MatchRecord> findByRequesterIdOrPartnerId(Long requesterId, Long partnerId);
    Page<MatchRecord> findByRequesterIdOrPartnerId(Long requesterId, Long partnerId, Pageable pageable);
    boolean existsByRequesterIdAndPartnerId(Long requesterId, Long partnerId);
    List<MatchRecord> findByStatusAndCreatedAtBefore(MatchStatus status, LocalDateTime cutoff);

    /** 채팅 대화방 목록 — 내가 참여한 특정 상태(ACCEPTED)의 매칭 (최근 순) */
    @Query("select m from MatchRecord m where m.status = :status and (m.requesterId = :userId or m.partnerId = :userId) order by m.createdAt desc")
    List<MatchRecord> findAcceptedRoomsOf(MatchStatus status, Long userId);
}
