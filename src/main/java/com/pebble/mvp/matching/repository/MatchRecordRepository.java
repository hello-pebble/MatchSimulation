package com.pebble.mvp.matching.repository;

import com.pebble.mvp.matching.domain.MatchRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRecordRepository extends JpaRepository<MatchRecord, Long> {
    List<MatchRecord> findByRequesterIdOrPartnerId(Long requesterId, Long partnerId);
    Page<MatchRecord> findByRequesterIdOrPartnerId(Long requesterId, Long partnerId, Pageable pageable);
    boolean existsByRequesterIdAndPartnerId(Long requesterId, Long partnerId);
}
