package com.pebble.mvp.service;

import com.pebble.mvp.domain.UserProfile;
import com.pebble.mvp.dto.AIResponse;
import com.pebble.mvp.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AIService {
    private final UserProfileRepository userProfileRepository;

    public String conductInterview(Long userId, String answer) {
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        if (profileOpt.isPresent()) {
            UserProfile profile = profileOpt.get();
            String analysis = "AI Analysis for answer: '" + answer + "'. User seems polite and expressive.";
            profile.setAiAnalysisResult(analysis);
            userProfileRepository.save(profile);
            return analysis;
        }
        return "User profile not found.";
    }

    public AIResponse simulateConversation(Long userId, String message) {
        return AIResponse.builder()
                .response("This is a mock AI response to: " + message)
                .feedback("Your conversation style is friendly. Try to ask more open-ended questions.")
                .build();
    }
}
