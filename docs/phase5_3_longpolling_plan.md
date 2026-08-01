# Phase 5-3 시작 전 계획 문서 — 채팅 Long Polling (서버 대기 응답)

작성일: 2026-08-01
선행 단계: Phase 5-2 (Short Polling) 완료 — 실측 빈 응답 90%

## 1. 배경과 목표

Short Polling은 새 메시지가 없어도 3초마다 요청이 발생한다(실측 빈 응답 90%),
그리고 전달 지연이 최대 폴링 주기(3초)만큼 생긴다. **Long Polling**으로 개선한다:

- 클라이언트가 요청하면 서버는 **바로 응답하지 않고 대기**한다
- 새 메시지 발생 → **즉시 응답** (지연 ≈ 0)
- 끝까지 없음 → 타임아웃(기본 20초, 최대 30초) 후 **빈 배열** 응답 → 클라이언트 재요청

## 2. 설계

| 구성요소 | 내용 |
| :--- | :--- |
| `GET /api/chat/{matchId}/messages/poll?afterId=N&timeoutSeconds=20` | Long Polling 엔드포인트. 즉시 데이터가 있으면 바로 반환 |
| `DeferredResult<List<ChatMessageResponse>>` | 서블릿 스레드를 점유하지 않는 비동기 응답. 타임아웃 시 빈 배열 |
| `ChatPollRegistry` | matchId별 대기자 큐 (`ConcurrentHashMap<Long, Queue<Waiter>>`). 타임아웃/완료 시 자동 제거 |
| 알림 지점 | 컨트롤러가 `send()` **트랜잭션 커밋 이후** `registry.publish(matchId)` 호출 → 대기자 각자의 afterId로 조회해 완료 |
| Virtual Threads | `spring.threads.virtual.enabled=true` (기존 설정) — 대기 요청의 스레드 비용 최소화 |
| 콘솔 | 수신 모드 선택(Short/Long) + 토글 하나로 통합. Long 모드는 응답 즉시 재요청 루프 |

주의: publish를 send 트랜잭션 안에서 호출하면 대기자가 커밋 전 데이터를 조회해
새 메시지를 놓칠 수 있다 → 커밋 경계 밖(컨트롤러)에서 호출한다.

## 3. 엣지케이스 정의

| # | 케이스 | 기대 동작 |
| :--- | :--- | :--- |
| E1 | poll 시점에 이미 새 메시지 존재 | 대기 없이 즉시 응답 |
| E2 | 대기 중 상대가 전송 | 전송 직후 응답 완료 (지연 ≈ 0) |
| E3 | 타임아웃까지 새 메시지 없음 | timeoutSeconds 후 200 + 빈 배열 |
| E4 | timeoutSeconds 범위 밖(0, 31 등) | 1~30초로 보정 |
| E5 | 비참여자/없는 매칭/미성사 매칭 poll | 403/404/400 즉시 응답 (대기하지 않음) |
| E6 | 같은 방을 여러 클라이언트가 poll | 전송 시 대기자 전원 각자의 afterId 기준으로 완료 |
| E7 | 대기자 타임아웃 후 전송 | 제거된 대기자는 완료 대상에서 제외 (누수 없음) |

## 4. 테스트 계획 (QA)

- `ChatLongPollTest`(MockMvc async): 즉시 응답(E1), 대기 중 전송 → 완료(E2), 권한 오류(E5), 대기자 정리(E7)
- curl 실측: E2 응답 시간(≈ 전송 시점), E3 타임아웃 시간(≈ timeoutSeconds), Short vs Long 요청 수 비교
- 기존 41건 전체 회귀

## 5. 산출물

- 코드: poll 엔드포인트, ChatPollRegistry, 콘솔 수신 모드 선택
- 문서: 본 계획, `phase5_3_longpolling_report.md`(Short vs Long 비교 실측), user_mode, README
