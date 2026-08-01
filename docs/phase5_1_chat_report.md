# Phase 5-1 완료 보고 문서 — 1:1 채팅 MVP (새로고침 + afterId 증분 조회)

완료일: 2026-08-01
계획 문서: [phase5_1_chat_plan.md](phase5_1_chat_plan.md)

## 1. 결과 요약 — 계획 대비 전 항목 완료

| 항목 | 결과 |
| :--- | :--- |
| chat 모듈 | ✅ `com.pebble.mvp.chat` (domain/repository/service/controller/dto) 신설 |
| 메시지 모델 | ✅ `ChatMessage` — id(단조 증가 커서), matchId, senderId, content(≤500자), createdAt |
| 대화방 | ✅ 별도 Room 없이 **ACCEPTED MatchRecord.id 재사용** |
| Flyway V3 | ✅ `V3__create_chat_messages.sql` + `(match_id, id)` 인덱스 — 기동 로그 "now at version v3" |
| 증분 조회 | ✅ `GET .../messages?afterId=N` — afterId 이후만 반환 (전체 재조회 문제 차단) |
| 콘솔 | ✅ 채팅 섹션 — 대화방 목록 / 말풍선 뷰 / [전송] / **[새로고침]**(afterId 자동 관리) |
| 시드 | ✅ 첫 ACCEPTED 매칭에 샘플 대화 3건 |

## 2. 엣지케이스 검증 결과 (2026-08-01 curl 실측)

| # | 케이스 | 기대 | 결과 |
| :--- | :--- | :--- | :--- |
| E1 | afterId=3 증분 조회 | id>3 메시지만 | ✅ 새 답장 1건만 반환 |
| E2 | 비참여자(male1)의 조회/전송 | 403 | ✅ "본인이 참여한 매칭의 대화만 이용할 수 있습니다." |
| E3 | REQUESTED 매칭에 전송 | 400 | ✅ "성사(ACCEPTED)된 매칭에서만 대화할 수 있습니다. 현재 상태: REQUESTED" |
| E4 | 없는 matchId | 404 | ✅ "매칭을 찾을 수 없습니다: 999999" |
| E5 | 빈 내용 / 501자 | 400 | ✅ Bean Validation 메시지 |
| E6 | afterId=마지막 id | 빈 배열 | ✅ `[]` |
| E7 | 상대가 보낸 메시지 | mine=false 수신 | ✅ 새로고침 시 senderName과 함께 수신 |
| - | 토큰 없음 | 401 | ✅ 기존 Security 규칙 적용 |

## 3. QA 테스트 체크리스트

- [x] `ChatIntegrationTest` 6건: 전송→증분 조회 왕복, 권한(403/400/404), 검증(400), 대화방 목록
- [x] `FlywayMigrationTest`에 V3 이력 검증 추가
- [x] 전체 **41건** 회귀 통과 (테스트 생성 데이터는 정리하여 공유 컨텍스트 보호)
- [x] curl 실측: 두 계정 양방향 대화 + 전 엣지케이스

## 4. API 명세 (요약 — 상세는 user_mode.md)

| 메서드/경로 | 설명 |
| :--- | :--- |
| GET /api/chat/rooms | 내 대화방(ACCEPTED 매칭) 목록 — 상대 이름, 마지막 메시지 |
| POST /api/chat/{matchId}/messages | 메시지 전송 (1~500자) |
| GET /api/chat/{matchId}/messages?afterId=N | afterId 이후 메시지 오름차순 (생략 시 전체) |

## 5. 다음 단계와의 연결

- 본 단계의 `afterId` 계약은 **Phase 5-2 Short Polling**(3초 자동 조회),
  **Phase 5-3 Long Polling**(서버 대기)에서 그대로 재사용된다 — 백엔드 조회 모델 변경 없음
- 사용자가 [새로고침]을 눌러야 하는 불편 → 다음 단계에서 클라이언트 자동화(Short Polling)로 해소
