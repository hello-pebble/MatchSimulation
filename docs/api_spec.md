# API 명세서

Base URL: `http://localhost:8080`
인증: 로그인 응답의 토큰을 `X-AUTH-TOKEN` 헤더에 담아 호출.
에러 응답(공통): `{"status": 400|401|403|404, "message": "..."}`

---

## 1. 인증 (Auth)

### POST /api/auth/signup — 회원가입
```json
// Request
{"email":"new@match.com","password":"p1","name":"신규","age":27,
 "gender":"FEMALE","job":"개발자","location":"서울"}
// Response 201
{"id":22,"email":"new@match.com","name":"신규","age":27,"gender":"FEMALE",
 "job":"개발자","location":"서울","role":"USER","status":"PENDING","createdAt":"..."}
```

### POST /api/auth/login — 로그인
```json
// Request
{"email":"male1@match.com","password":"pass1234"}
// Response 200
{"token":"550e8400-...","user":{ ...UserResponse }}
```
- 401: 이메일/비밀번호 불일치 · 403: 정지 계정

### GET /api/auth/me — 내 정보 🔒
Response 200: `UserResponse`

---

## 2. 매칭 (Matching) 🔒

### GET /api/matching/recommendations — 추천 목록
- ACTIVE 회원만 호출 가능 (아니면 403). 상위 5명 반환.
```json
// Response 200
[{"userId":11,"name":"정하윤","age":24,"gender":"FEMALE","job":"디자이너",
  "location":"인천","score":70.0,"reason":"같은 지역(인천), 나이 차이 2세"}]
```

### POST /api/matching/requests — 매칭 요청
```json
// Request
{"partnerId":22}
// Response 201
{"id":31,"requesterId":2,"requesterName":"김민준","partnerId":22,
 "partnerName":"신규","status":"REQUESTED","score":35.0,"createdAt":"..."}
```
- 400: 자기 자신 / 비활성 상대 / 중복 요청 · 404: 상대 없음

### POST /api/matching/requests/{matchId}/respond — 수락/거절
```json
// Request
{"accept":true}
// Response 200 — status가 ACCEPTED 또는 REJECTED로 변경된 MatchResponse
```
- 403: 요청 받은 본인이 아님 · 400: 이미 처리됨

### GET /api/matching/my — 내 매칭 목록
Response 200: `MatchResponse[]`

---

## 3. Q&A / 알림 (User) 🔒

### POST /api/qna — 문의 등록
```json
// Request
{"title":"문의드립니다","question":"매칭 관련 문의입니다."}
// Response 201
{"id":4,"userId":2,"userName":"김민준","title":"...","question":"...",
 "answer":null,"status":"WAITING","createdAt":"...","answeredAt":null}
```

### GET /api/qna/my — 내 문의 목록
Response 200: `QnaResponse[]` (최신순)

### GET /api/notifications/my — 내 알림
Response 200: 전체 공지 + 본인 대상 알림 (최신순)
```json
[{"id":3,"targetUserId":null,"target":"ALL","title":"공지","message":"...","createdAt":"..."}]
```

---

## 4. 관리자 (Admin) 🔒 ADMIN 전용 (아니면 403)

### GET /api/admin/users — 전체 회원 목록
Response 200: `UserResponse[]`

### PATCH /api/admin/users/{userId}/status — 회원 상태 변경 (승인/정지)
```json
// Request
{"status":"ACTIVE"}   // PENDING | ACTIVE | SUSPENDED
// Response 200 — 변경된 UserResponse
```

### GET /api/admin/qna?status=WAITING — 문의 목록
- `status` 생략 시 전체, `WAITING`/`ANSWERED` 필터 가능

### POST /api/admin/qna/{qnaId}/answer — 답변 작성
```json
// Request
{"answer":"확인했습니다"}
// Response 200 — status=ANSWERED, answeredAt 채워진 QnaResponse
```

### POST /api/admin/notifications — 알림 생성
```json
// Request (targetUserId=null 이면 전체 공지)
{"targetUserId":null,"title":"점검 안내","message":"오늘 밤 12시 점검"}
// Response 201 — NotificationResponse
```

### GET /api/admin/notifications — 등록된 알림 목록

### GET /api/admin/stats/matches — 매칭 현황 통계
```json
// Response 200
{"totalMatches":31,"acceptedMatches":11,"acceptanceRate":35.5,
 "daily":{"2026-07-17":1,"2026-07-24":4},
 "byGender":{"FEMALE":10,"MALE":21},
 "byStatus":{"ACCEPTED":11,"REJECTED":10,"REQUESTED":10}}
```

---

## 5. 외부 AI 매칭 서버 계약 (연동 시)

`application.yml`에서 `matching.engine: external-ai` 설정 시 아래 계약으로 호출:

### POST {matching.ai.base-url}/api/v1/recommend
```json
// Request
{"userId":1,
 "profile":{"userId":1,"name":"김민준","age":30,"gender":"MALE","job":"개발자","location":"서울"},
 "candidates":[{"userId":2,"name":"...","age":29,"gender":"FEMALE","job":"...","location":"..."}]}
// Response — 점수 내림차순 권장
[{"userId":2,"name":"...","age":29,"gender":"FEMALE","job":"...",
  "location":"...","score":87.5,"reason":"AI 유사도 분석"}]
```
