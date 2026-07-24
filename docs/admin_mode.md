# 관리자 모드 문서 (Admin Mode)

대상: 관리자 기능 — 회원관리, QnA 답변, 알림 등록, 매칭 현황 통계
콘솔: `http://localhost:8080/admin.html`
사용자 모드 문서: [user_mode.md](user_mode.md)

기본 관리자 계정: `admin@match.com` / `admin1234`
모든 관리자 API는 토큰의 Role이 `ADMIN`이 아니면 **403**.

---

## 1. 기능 명세

| 기능 | 내용 |
| :--- | :--- |
| 회원 관리 | 전체 회원 목록 조회, 상태 변경 — `PENDING`(가입 대기) / `ACTIVE`(승인) / `SUSPENDED`(정지) |
| Q&A 관리 | 전체/상태별 문의 목록 조회, 답변 작성(→ `ANSWERED`, 답변 시각 기록) |
| 알림 등록 | 전체 공지(대상 미지정) 또는 개별 회원 대상 알림 생성, 등록된 알림 목록 조회 |
| 매칭 통계 | 전체/성사 건수, 성사율(%), 일별·성별(요청자 기준)·상태별 매칭 건수 요약 |

## 2. API 명세

Base URL: `http://localhost:8080` · 모든 API는 관리자 토큰 `X-AUTH-TOKEN` 필요
에러 응답(공통): `{"status": 400|401|403|404, "message": "..."}`

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

## 3. 콘솔 사용 시나리오 (`/admin.html`)

1. `admin@match.com` / `admin1234`로 **로그인**
2. **전체 회원 조회** → PENDING 회원의 userId로 **상태 변경(승인)**
3. **답변 대기** 조회 → qnaId 입력 후 **답변 등록**
4. **알림 생성** — 대상 userId를 비우면 전체 공지, 지정하면 개별 알림
5. **통계 새로고침** → 전체/성사/성사율 카드와 일별·성별·상태별 막대그래프 확인

## 4. 외부 AI 매칭 서버 계약 (연동 시)

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
