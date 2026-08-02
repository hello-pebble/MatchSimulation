# Phase 6 시작 전 계획 문서 — 채팅 WebSocket (실시간 양방향)

작성일: 2026-08-01
선행 단계: [phase5_3_longpolling_report.md](phase5_3_longpolling_report.md) (PR #14 머지 완료)

## 1. 목표

채팅 수신 방식 진화의 마지막 단계 — **새로고침 → Short Polling → Long Polling → WebSocket**.
Long Polling은 "응답 후 재요청" 반복이 남는다. WebSocket은 **연결 1개를 유지**하며
서버가 새 메시지를 즉시 push하고, 클라이언트도 같은 연결로 전송할 수 있는
**양방향** 채널이다.

| 항목 | Long Polling | WebSocket |
| :--- | :--- | :--- |
| 연결 | 요청마다 새 HTTP 요청 (대기 후 재요청) | **최초 1회 핸드셰이크 후 연결 유지** |
| 방향 | 단방향 수신 (전송은 별도 POST) | **양방향** — 수신·전송 모두 같은 연결 |
| 전달 지연 | ≈ 0 (재요청 공백 존재) | ≈ 0 (공백 없음) |
| 요청 수 | 메시지 수신마다 재요청 1회 | 0 (연결 유지) |

## 2. 설계 결정

### 2.1 순수 WebSocket (STOMP 미사용)

`TextWebSocketHandler` 기반 순수 WebSocket으로 구현하고 STOMP는 쓰지 않는다.

- 콘솔이 외부 라이브러리 없는 vanilla JS라 브라우저 내장 `WebSocket` API로 충분
- 구독 대상이 `matchId` 하나뿐이라 브로커/구독 프로토콜 계층이 과함
- 핸드셰이크·세션 관리·브로드캐스트를 직접 다뤄 저수준 동작을 드러낸다
  (포트폴리오 관점 — Long Polling의 `DeferredResult` 직접 구현과 같은 결)

### 2.2 엔드포인트와 인증

- `ws://host/ws/chat?matchId={id}&token={JWT}` — 브라우저 WebSocket API는
  커스텀 헤더를 못 실으므로 JWT를 쿼리 파라미터로 전달
- `HandshakeInterceptor`에서 연결 **수립 전** 검증: JWT 유효성(기존 `JwtProvider` 재사용),
  정지 계정 차단, 매칭 존재/ACCEPTED/참여자 검증(기존 `ChatService.verifyParticipant` 재사용)
  → 실패 시 핸드셰이크 거부(401/403/404)
- Security 필터 체인에는 `/ws/**` permitAll — HTTP 인증 대신 인터셉터가 담당
  (기존 API의 X-AUTH-TOKEN 헤더 방식은 변경 없음)

### 2.3 세션 레지스트리와 브로드캐스트

- `ChatSessionRegistry` — `ConcurrentHashMap<matchId, Set<WebSocketSession>>`.
  연결 수립 시 등록, 종료/에러 시 제거 (Long Polling의 `ChatPollRegistry`와 대칭 구조)
- 메시지 저장 성공 시 해당 matchId의 모든 세션에 `ChatMessageResponse` JSON push.
  `mine` 필드는 수신자 기준이므로 **세션별로 직렬화** (세션 attributes에 userId 보관)
- **수신 경로 교차 호환**: REST POST 전송도 WebSocket 세션에 push하고,
  WebSocket 전송도 Long Polling 대기자를 깨운다(`ChatPollRegistry.publish`)
  — 두 사용자가 서로 다른 수신 모드를 써도 실시간성 유지

### 2.4 양방향 전송

- 클라이언트 → 서버 텍스트 프레임: `{"content":"..."}` → 기존 `ChatService.send`
  재사용(권한·검증·저장 동일) 후 브로드캐스트
- 검증 실패(빈 내용/500자 초과 등)는 `{"error":"..."}` 프레임으로 회신하고 연결 유지

## 3. 구현 범위

| 항목 | 내용 |
| :--- | :--- |
| 의존성 | `spring-boot-starter-websocket` 추가 |
| 신규 | `chat/websocket/ChatWebSocketConfig`, `ChatHandshakeInterceptor`, `ChatWebSocketHandler`, `ChatSessionRegistry` |
| 수정 | `ChatController.send` — 커밋 후 WebSocket 브로드캐스트 추가, `SecurityConfig` — `/ws/**` permitAll, `ChatService` — verifyParticipant 접근 허용 |
| 콘솔 | 수신 모드에 **WebSocket** 추가 — 연결 유지 표시, WS 모드에서는 전송도 소켓으로 |
| DB | 변경 없음 (메시지 모델 그대로 — Flyway 마이그레이션 없음) |

## 4. 테스트 계획 (ChatWebSocketTest)

`@SpringBootTest(RANDOM_PORT)` + `StandardWebSocketClient` 실연결 테스트:

1. 연결 후 상대가 REST로 전송 → 프레임 즉시 수신 (교차 호환)
2. 소켓으로 전송 → 저장 확인 + 양측 세션 수신, `mine` 구분
3. 토큰 없음/위조 → 핸드셰이크 거부
4. 비참여자 토큰 → 핸드셰이크 거부
5. 연결 종료 → 레지스트리에서 세션 정리 확인 (누수 없음)

엣지케이스 실측(curl/node): push 지연 측정, Long Polling과 비교표,
검증 실패 프레임(`error`), 잘못된 JSON 프레임.

## 5. 완료 기준

- 전체 테스트(기존 45건 + 신규) green
- 콘솔에서 두 계정이 WebSocket 모드로 실시간 대화 확인
- 완료 문서(4단계 진화 최종 비교표 포함) + `user_mode.md`/`README.md` 갱신
- 독립 PR 생성 → 머지
