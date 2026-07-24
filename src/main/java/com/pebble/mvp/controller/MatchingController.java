package com.pebble.mvp.controller;

import com.pebble.mvp.dto.MatchingDtos.MatchRequestDto;
import com.pebble.mvp.dto.MatchingDtos.MatchRespondDto;
import com.pebble.mvp.dto.MatchingDtos.MatchResponse;
import com.pebble.mvp.service.AuthService;
import com.pebble.mvp.service.MatchingService;
import com.pebble.mvp.service.matching.ScoredCandidate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final AuthService authService;
    private final MatchingService matchingService;

    @GetMapping("/recommendations")
    public List<ScoredCandidate> recommendations(@RequestHeader("X-AUTH-TOKEN") String token) {
        return matchingService.recommend(authService.authenticate(token));
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse request(@RequestHeader("X-AUTH-TOKEN") String token,
                                 @Valid @RequestBody MatchRequestDto request) {
        return matchingService.request(authService.authenticate(token), request.partnerId());
    }

    @PostMapping("/requests/{matchId}/respond")
    public MatchResponse respond(@RequestHeader("X-AUTH-TOKEN") String token,
                                 @PathVariable Long matchId,
                                 @Valid @RequestBody MatchRespondDto request) {
        return matchingService.respond(authService.authenticate(token), matchId, request.accept());
    }

    @GetMapping("/my")
    public List<MatchResponse> myMatches(@RequestHeader("X-AUTH-TOKEN") String token) {
        return matchingService.myMatches(authService.authenticate(token));
    }
}
