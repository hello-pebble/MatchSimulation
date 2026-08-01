# Phase 5-1 시작 전 계획 문서 — 1:1 채팅 MVP (새로고침 + afterId 증분 조회)

작성일: 2026-08-01
선행 단계: Phase 4-3 (캐싱 + 만료 스케줄러) 완료

## 1. 배경과 목표

매칭이 성사(ACCEPTED)된 사용자 간 대화 기능을 단계적으로 진화시킨다:

1. **새로고침 기반** (본 단계) — 사용자가 [새로고침]을 누르면 조회
2. Short Polling (Phase 5-2) — 클라이언트가 3초 주기로 자동 조회
3. Long Polling (Phase 5-3) — 서버가 새 메시지가 생길 때까지 대기 후 응답

본 단계는 이후 어떤 전달 방식으로 진화해도 흔들리지 않을 **메시지 모델과 조회
계약을 먼저 고정**하는 것이 목표다. 특히 "매번 전체 메시지를 다시 가져오는" 문제를
**마지막 조회 메시지 ID(afterId) 증분 조회**로 처음부터 차단한다 — 이 계약은
Short/Long Polling에서도 그대로 재사용된다.

## 2. 설계

### 2.1 도메인 — `chat` 모듈 신설 (package-by-feature 유지)

| 구성요소 | 내용 |
| :--- | :--- |
| `ChatMessage` | id(PK, 단조 증가 — afterId 커서로 사용), matchId, senderId, content(≤500자), createdAt |
| 대화방 식별 | 별도 Room 엔티티 없이 **ACCEPTED 상태의 MatchRecord.id를 방 ID로 재사용** |
| `V3__create_chat_messages.sql` | chat_messages 테이블 + `(match_id, id)` 인덱스 — 증분 조회 최적화 |
| 권한 규칙 | matchId의 매칭이 ACCEPTED이고 내가 참여자(requester/partner)일 때만 전송·조회 가능 |

### 2.2 API

| 메서드/경로 | 설명 |
| :--- | :--- |
| GET /api/chat/rooms | 내 대화방 목록 = 내 ACCEPTED 매칭 (상대 이름, 마지막 메시지 포함) |
| POST /api/chat/{matchId}/messages | 메시지 전송 (내용 1~500자) |
| GET /api/chat/{matchId}/messages?afterId=N | **afterId보다 큰 메시지만** 오름차순 반환 (생략 시 전체) |

에러: 404 매칭 없음 · 403 참여자 아님 · 400 ACCEPTED 아님(REQUESTED/REJECTED/EXPIRED)·빈 내용·500자 초과

### 2.3 콘솔 (`index.html`)

채팅 섹션 추가: [대화방 목록] → matchId 선택 → 메시지 입력 + [전송] + **[새로고침]**.
새로고침은 마지막 수신 메시지 id를 기억해 `afterId`로 증분 요청하고 화면에 이어붙인다.

### 2.4 시드 데이터

첫 ACCEPTED 매칭에 샘플 대화 3건을 적재해 콘솔에서 즉시 확인 가능하게 한다.

## 3. 엣지케이스 정의

| # | 케이스 | 기대 동작 |
| :--- | :--- | :--- |
| E1 | afterId 증분 조회 | afterId 이후 메시지만 반환, 이전 메시지 재전송 없음 |
| E2 | 참여자가 아닌 사용자의 전송/조회 | 403 |
| E3 | REQUESTED/REJECTED/EXPIRED 매칭에 전송 | 400 "성사된 매칭에서만 대화할 수 있습니다" |
| E4 | 존재하지 않는 matchId | 404 |
| E5 | 빈 내용 / 500자 초과 | 400 (Bean Validation) |
| E6 | afterId가 마지막 메시지 id와 같음 | 빈 배열 (새 메시지 없음) |
| E7 | 상대가 전송한 메시지 | 내 다음 새로고침에서 수신, `mine=false` 구분 |

## 4. 테스트 계획 (QA)

- `ChatIntegrationTest`: 전송→조회 왕복, afterId 증분(E1·E6), 권한(E2·E3·E4), 검증(E5),
  양방향 대화(E7) — 생성 데이터는 정리하여 공유 컨텍스트 보호
- Flyway V3 이력 검증(기존 FlywayMigrationTest 확장), 전체 회귀
- curl 실측: 두 계정(male/female)으로 전송·증분 조회 실측

## 5. 산출물

- 코드: chat 모듈(domain/repository/service/controller/dto), V3 SQL, 콘솔 채팅 섹션, 시드
- 문서: 본 계획, `phase5_1_chat_report.md`, user_mode(채팅 명세), README
