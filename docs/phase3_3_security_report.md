# Phase 3-3 완료 보고 문서 — Spring Security + JWT + BCrypt

완료일: 2026-07-30
계획 문서: [phase3_3_security_plan.md](phase3_3_security_plan.md)

## 1. 결과 요약 — 계획 대비 전 항목 완료

| 항목 | 결과 |
| :--- | :--- |
| Spring Security 필터 체인 | ✅ `SecurityConfig` — URL 인가 규칙, stateless, csrf off |
| JWT (HS256) | ✅ `JwtProvider` — sub=userId, role, 만료 60분 (`app.jwt.*` 설정) |
| 인증 필터 | ✅ `JwtAuthFilter` — `X-AUTH-TOKEN` 헤더 유지 (콘솔 JS 무변경) |
| BCrypt 해싱 | ✅ signup/login + 시드 데이터 인코딩 |
| `TokenStore` 제거 | ✅ 삭제 — 무상태 JWT로 대체 (서버 재시작에도 토큰 유효) |
| 관리자 인가 | ✅ `/api/admin/** → hasRole('ADMIN')` (컨트롤러의 requireAdmin 제거) |
| 컨트롤러 정리 | ✅ `@RequestHeader` 토큰 파라미터 → `@AuthenticationPrincipal User` 주입 |
| 에러 포맷 유지 | ✅ EntryPoint 401 / AccessDeniedHandler 403 → `{status, message}` JSON |

### Spring Boot 4 대응 사항 (트러블슈팅 기록)

1. **H2 콘솔 모듈 분리**: Boot 4에서 H2 콘솔 자동설정이 `spring-boot-h2console`
   별도 모듈로 분리됨 — runtimeOnly 의존성 추가로 해결 (이전까지 /h2-console이
   실제로는 미등록 상태였음을 이번에 발견·수정)
2. **별도 서블릿 경로 매칭**: H2 콘솔은 DispatcherServlet 밖의 서블릿이라
   문자열 `requestMatchers("/h2-console/**")`가 매칭되지 않음 —
   `PathPatternRequestMatcher.withDefaults().matcher(...)`로 해결

## 2. 엣지케이스 검증 결과 (2026-07-30 curl 실측 + 자동 테스트)

| # | 케이스 | 기대 | 결과 |
| :--- | :--- | :--- | :--- |
| E1 | 토큰 없음 | 401 JSON | ✅ `{"status":401,"message":"유효하지 않은 토큰입니다..."}` |
| E2 | 위조 서명 (`aaa.bbb.ccc`, 타 키 서명) | 401 | ✅ curl + `JwtProviderTest` |
| E3 | 만료 토큰 | 401 | ✅ `JwtProviderTest` (만료 즉시 거부) |
| E4 | USER → /api/admin/** | 403 | ✅ `{"status":403,"message":"관리자 권한이 필요합니다."}` |
| E5 | 정지 계정의 유효 토큰 | 401 (즉시 차단) | ✅ `SecurityIntegrationTest` (정지 후 기존 토큰 무효) |
| E6 | 정지 계정 로그인 | 403 | ✅ "정지된 계정입니다" |
| E7 | 비밀번호 저장 | BCrypt 해시 | ✅ `$2a$...` 저장, 응답에 비노출 |
| E8 | 콘솔/h2-console 접근 | permitAll | ✅ index/admin 200, /h2-console 200 |

## 3. QA 테스트 체크리스트

### 자동 (신규 12건 — 전체 28건 통과)
- [x] JwtProviderTest 4건: 발급/복원, 만료 거부, 타 키 서명 거부, 변조·빈 토큰 거부
- [x] SecurityIntegrationTest 8건: 무토큰 401, 위조 401, 로그인→JWT 인증 성공(3-세그먼트 검증),
      USER→admin 403, ADMIN 200, BCrypt 저장, 정지 계정 토큰 401, 정적 리소스 permitAll
- [x] 기존 테스트 회귀 통과 (페이징 7, 트랜잭션 5, 엔진 2, 컨텍스트 2)

### 수동 (curl + 콘솔 실측)
- [x] 로그인 → JWT 발급 → /api/auth/me, 매칭 요청까지 정상 (콘솔 JS 무변경으로 동작)
- [x] 잘못된 비밀번호 401, 정지 계정 로그인 403
- [x] /h2-console 접속 200 (로그인 화면), 콘솔 페이지 200
- [x] 관리자 API — ADMIN 토큰만 접근 가능

## 4. API 명세 변경 (요약)

- 인증 방식: `X-AUTH-TOKEN` 헤더에 **JWT** (로그인 응답 `token` — `header.payload.signature`)
- 토큰 만료: 60분 (만료 후 401 → 재로그인)
- 401/403 응답은 기존 `{status, message}` 포맷 유지
- 신규 설정: `app.jwt.secret`(운영 시 환경변수 주입 권장), `app.jwt.expiry-minutes`

## 5. Phase 3 (전통 백엔드 요소) 전체 완료

| 단계 | 내용 | PR |
| :--- | :--- | :--- |
| 3-1 | 페이징/정렬 (Pageable + PageResponse) | #4 |
| 3-2 | 트랜잭션 경계 + 낙관적 락 (409) | #5 |
| 3-3 | Spring Security + JWT + BCrypt | 본 단계 |
