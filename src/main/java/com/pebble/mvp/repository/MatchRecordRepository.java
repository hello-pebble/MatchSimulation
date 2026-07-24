package com.pebble.mvp.repository;

import com.pebble.mvp.domain.MatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRecordRepository extends JpaRepository<MatchRecord, Long> {
    List<MatchRecord> findByRequesterIdOrPartnerId(Long requesterId, Long partnerId);
    boolean existsByRequesterIdAndPartnerId(Long requesterId, Long partnerId);
}
