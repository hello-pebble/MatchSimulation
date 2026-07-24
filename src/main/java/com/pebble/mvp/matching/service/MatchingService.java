package com.pebble.mvp.matching.service;

import com.pebble.mvp.common.ApiException;
import com.pebble.mvp.matching.domain.MatchRecord;
import com.pebble.mvp.user.domain.User;
import com.pebble.mvp.matching.domain.MatchStatus;
import com.pebble.mvp.user.domain.UserStatus;
import com.pebble.mvp.matching.dto.MatchingDtos.MatchResponse;
import com.pebble.mvp.matching.repository.MatchRecordRepository;
import com.pebble.mvp.user.repository.UserRepository;
import com.pebble.mvp.matching.engine.MatchingEngine;
import com.pebble.mvp.matching.engine.ScoredCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private static final int RECOMMENDATION_LIMIT = 5;

    private final UserRepository userRepository;
    private final MatchRecordRepository matchRecordRepository;
    private final MatchingEngine matchingEngine;

    /** ACTIVE 상태의 이성 회원을 후보로 매칭 엔진에 위임 */
    public List<ScoredCandidate> recommend(User me) {
        if (me.getStatus() != UserStatus.ACTIVE) {
            throw ApiException.forbidden("승인(ACTIVE)된 회원만 추천을 받을 수 있습니다. 현재 상태: " + me.getStatus());
        }
        List<User> candidates = userRepository.findByStatus(UserStatus.ACTIVE).stream()
                .filter(u -> !Objects.equals(u.getId(), me.getId()))
                .filter(u -> u.getGender() != null && u.getGender() != me.getGender())
                .toList();
        return matchingEngine.recommend(me, candidates).stream()
                .limit(RECOMMENDATION_LIMIT)
                .toList();
    }

    @Transactional
    public MatchResponse request(User me, Long partnerId) {
        if (Objects.equals(me.getId(), partnerId)) {
            throw ApiException.badRequest("자기 자신에게는 매칭을 요청할 수 없습니다.");
        }
        User partner = userRepository.findById(partnerId)
                .orElseThrow(() -> ApiException.notFound("상대 회원을 찾을 수 없습니다: " + partnerId));
        if (partner.getStatus() != UserStatus.ACTIVE) {
            throw ApiException.badRequest("상대 회원이 활성 상태가 아닙니다.");
        }
        if (matchRecordRepository.existsByRequesterIdAndPartnerId(me.getId(), partnerId)) {
            throw ApiException.badRequest("이미 매칭을 요청한 상대입니다.");
        }
        double score = matchingEngine.recommend(me, List.of(partner)).stream()
                .findFirst()
                .map(ScoredCandidate::score)
                .orElse(0.0);
        MatchRecord record = matchRecordRepository.save(MatchRecord.builder()
                .requesterId(me.getId())
                .partnerId(partnerId)
                .status(MatchStatus.REQUESTED)
                .score(score)
                .createdAt(LocalDateTime.now())
                .build());
        return toResponse(record);
    }

    /** 매칭 요청을 받은 상대(partner)만 수락/거절할 수 있다 */
    @Transactional
    public MatchResponse respond(User me, Long matchId, boolean accept) {
        MatchRecord record = matchRecordRepository.findById(matchId)
                .orElseThrow(() -> ApiException.notFound("매칭 요청을 찾을 수 없습니다: " + matchId));
        if (!Objects.equals(record.getPartnerId(), me.getId())) {
            throw ApiException.forbidden("본인이 받은 매칭 요청만 응답할 수 있습니다.");
        }
        if (record.getStatus() != MatchStatus.REQUESTED) {
            throw ApiException.badRequest("이미 처리된 매칭입니다. 상태: " + record.getStatus());
        }
        record.setStatus(accept ? MatchStatus.ACCEPTED : MatchStatus.REJECTED);
        return toResponse(record);
    }

    public List<MatchResponse> myMatches(User me) {
        return matchRecordRepository.findByRequesterIdOrPartnerId(me.getId(), me.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private MatchResponse toResponse(MatchRecord record) {
        Map<Long, String> names = Map.of(
                record.getRequesterId(), nameOf(record.getRequesterId()),
                record.getPartnerId(), nameOf(record.getPartnerId())
        );
        return MatchResponse.of(record, names.get(record.getRequesterId()), names.get(record.getPartnerId()));
    }

    private String nameOf(Long userId) {
        return userRepository.findById(userId).map(User::getName).orElse("(탈퇴 회원)");
    }
}
