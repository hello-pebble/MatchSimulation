package com.pebble.mvp.chat.service;

import com.pebble.mvp.chat.domain.ChatMessage;
import com.pebble.mvp.chat.dto.ChatDtos.ChatMessageResponse;
import com.pebble.mvp.chat.dto.ChatDtos.ChatRoomResponse;
import com.pebble.mvp.chat.repository.ChatMessageRepository;
import com.pebble.mvp.common.ApiException;
import com.pebble.mvp.matching.domain.MatchRecord;
import com.pebble.mvp.matching.domain.MatchStatus;
import com.pebble.mvp.matching.repository.MatchRecordRepository;
import com.pebble.mvp.user.domain.User;
import com.pebble.mvp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final MatchRecordRepository matchRecordRepository;
    private final UserRepository userRepository;

    /** 내 대화방 목록 = 내가 참여한 ACCEPTED 매칭 (최근 매칭 순, 마지막 메시지 포함) */
    public List<ChatRoomResponse> myRooms(User me) {
        return matchRecordRepository.findAcceptedRoomsOf(MatchStatus.ACCEPTED, me.getId()).stream()
                .map(record -> {
                    Long partnerId = Objects.equals(record.getRequesterId(), me.getId())
                            ? record.getPartnerId() : record.getRequesterId();
                    ChatMessage last = chatMessageRepository
                            .findTopByMatchIdOrderByIdDesc(record.getId()).orElse(null);
                    return new ChatRoomResponse(
                            record.getId(), partnerId, nameOf(partnerId),
                            last == null ? null : last.getContent(),
                            last == null ? null : last.getCreatedAt());
                })
                .toList();
    }

    @Transactional
    public ChatMessageResponse send(User me, Long matchId, String content) {
        verifyParticipant(me, matchId);
        ChatMessage saved = chatMessageRepository.save(ChatMessage.builder()
                .matchId(matchId)
                .senderId(me.getId())
                .content(content)
                .createdAt(LocalDateTime.now())
                .build());
        return ChatMessageResponse.of(saved, me.getName(), me.getId());
    }

    /**
     * afterId 증분 조회 — 마지막으로 받은 메시지 id 이후만 반환한다.
     * afterId가 null이면 전체 대화를 처음부터 반환한다 (최초 입장).
     */
    public List<ChatMessageResponse> messages(User me, Long matchId, Long afterId) {
        verifyParticipant(me, matchId);
        List<ChatMessage> messages = afterId == null
                ? chatMessageRepository.findByMatchIdOrderByIdAsc(matchId)
                : chatMessageRepository.findByMatchIdAndIdGreaterThanOrderByIdAsc(matchId, afterId);
        return messages.stream()
                .map(m -> ChatMessageResponse.of(m, nameOf(m.getSenderId()), me.getId()))
                .toList();
    }

    /** 매칭이 존재하고, ACCEPTED 상태이며, 내가 참여자인지 검증한다. */
    private void verifyParticipant(User me, Long matchId) {
        MatchRecord record = matchRecordRepository.findById(matchId)
                .orElseThrow(() -> ApiException.notFound("매칭을 찾을 수 없습니다: " + matchId));
        boolean participant = Objects.equals(record.getRequesterId(), me.getId())
                || Objects.equals(record.getPartnerId(), me.getId());
        if (!participant) {
            throw ApiException.forbidden("본인이 참여한 매칭의 대화만 이용할 수 있습니다.");
        }
        if (record.getStatus() != MatchStatus.ACCEPTED) {
            throw ApiException.badRequest("성사(ACCEPTED)된 매칭에서만 대화할 수 있습니다. 현재 상태: " + record.getStatus());
        }
    }

    private String nameOf(Long userId) {
        return userRepository.findById(userId).map(User::getName).orElse("(탈퇴 회원)");
    }
}
