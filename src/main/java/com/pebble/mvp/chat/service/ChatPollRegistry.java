package com.pebble.mvp.chat.service;

import com.pebble.mvp.chat.dto.ChatDtos.ChatMessageResponse;
import com.pebble.mvp.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Long Polling 대기자 레지스트리.
 * 새 메시지가 없을 때 요청을 붙잡아 두었다가(DeferredResult),
 * 해당 매칭에 메시지가 저장되면 대기자 각자의 afterId 기준으로 즉시 완료시킨다.
 * 타임아웃/완료된 대기자는 콜백으로 큐에서 제거되어 누수가 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPollRegistry {

    public record Waiter(User user, Long afterId, DeferredResult<List<ChatMessageResponse>> result) {}

    private final Map<Long, Queue<Waiter>> waiters = new ConcurrentHashMap<>();

    public void register(Long matchId, Waiter waiter) {
        Queue<Waiter> queue = waiters.computeIfAbsent(matchId, k -> new ConcurrentLinkedQueue<>());
        queue.add(waiter);
        waiter.result().onCompletion(() -> queue.remove(waiter));
        waiter.result().onTimeout(() -> queue.remove(waiter));
    }

    /**
     * 메시지 저장 트랜잭션 커밋 이후 호출된다 — 대기자가 커밋 전 데이터를 읽는 것을 막기 위해
     * 서비스(@Transactional) 밖(컨트롤러)에서 부른다.
     */
    public void publish(Long matchId, ChatService chatService) {
        Queue<Waiter> queue = waiters.get(matchId);
        if (queue == null) {
            return;
        }
        for (Waiter waiter : queue) {
            if (waiter.result().isSetOrExpired()) {
                continue;
            }
            List<ChatMessageResponse> news =
                    chatService.messages(waiter.user(), matchId, waiter.afterId());
            if (!news.isEmpty()) {
                waiter.result().setResult(news); // onCompletion에서 큐 제거
            }
        }
    }

    /** 테스트/관측용 — 현재 대기자 수 */
    public int waitingCount(Long matchId) {
        Queue<Waiter> queue = waiters.get(matchId);
        return queue == null ? 0 : queue.size();
    }
}
