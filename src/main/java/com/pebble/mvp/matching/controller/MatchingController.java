package com.pebble.mvp.matching.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.pebble.mvp.common.PageResponse;
import com.pebble.mvp.matching.dto.MatchingDtos.MatchRequestDto;
import com.pebble.mvp.matching.dto.MatchingDtos.MatchRespondDto;
import com.pebble.mvp.matching.dto.MatchingDtos.MatchResponse;
import com.pebble.mvp.matching.service.MatchingService;
import com.pebble.mvp.matching.engine.ScoredCandidate;
import com.pebble.mvp.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "매칭", description = "추천, 매칭 요청/응답, 내 매칭 이력")
@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    @GetMapping("/recommendations")
    public List<ScoredCandidate> recommendations(@AuthenticationPrincipal User me) {
        return matchingService.recommend(me);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse request(@AuthenticationPrincipal User me,
                                 @Valid @RequestBody MatchRequestDto request) {
        return matchingService.request(me, request.partnerId());
    }

    @PostMapping("/requests/{matchId}/respond")
    public MatchResponse respond(@AuthenticationPrincipal User me,
                                 @PathVariable Long matchId,
                                 @Valid @RequestBody MatchRespondDto request) {
        return matchingService.respond(me, matchId, request.accept());
    }

    @GetMapping("/my")
    public PageResponse<MatchResponse> myMatches(@AuthenticationPrincipal User me,
                                                 @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                                                 Pageable pageable) {
        return matchingService.myMatches(me, pageable);
    }
}
