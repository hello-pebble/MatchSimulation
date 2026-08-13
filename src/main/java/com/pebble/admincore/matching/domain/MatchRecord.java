package com.pebble.admincore.matching.domain;

import com.pebble.admincore.matching.domain.MatchStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long requesterId;

    @Column(nullable = false)
    private Long partnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    private Double score;

    /** 낙관적 락 — 동시 응답 경쟁 시 한쪽만 커밋 허용 */
    @Version
    private Long version;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
