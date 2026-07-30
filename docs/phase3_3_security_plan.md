# Phase 3-3 시작 전 계획 문서 — Spring Security + JWT + BCrypt

작성일: 2026-07-30
선행 단계: Phase 3-2 (트랜잭션/낙관적 락) 완료

## 1. 배경과 목표

현재 인증은 수제 `TokenStore`(UUID → userId 인메모리 맵)이고 비밀번호는 평문 저장이다.
표준 보안 스택으로 교체한다:

- **Spring Security** `SecurityFilterChain` — URL 인가 규칙, 표준 필터 체인
- **JWT** (HS256) — 무상태 토큰. 서버 재시작에도 유효, `TokenStore` 제거
- **BCrypt** — 비밀번호 해싱 (시드 데이터 포함)
- 인증 헤더는 기존 **`X-AUTH-TOKEN` 유지** (토큰 값만 JWT로 교체, 콘솔 무변경)

## 2. 설계

| 구성요소 | 내용 |
| :--- | :--- |
| `user/security/JwtProvider` | HS256 서명 발급/검증. claims: sub=userId, role. 만료 기본 60분. 설정: `app.jwt.secret`, `app.jwt.expiry-minutes` |
| `user/security/JwtAuthFilter` | `OncePerRequestFilter` — `X-AUTH-TOKEN` 파싱 → DB 사용자 조회 → `SecurityContext`에 인증 주입. 정지(SUSPENDED) 계정은 인증 거부 |
| `common/config/SecurityConfig` | permitAll: `/api/auth/signup`, `/api/auth/login`, 정적 리소스, `/h2-console/**` · `/api/admin/**` → `hasRole('ADMIN')` · 그 외 인증 필요 · csrf off, stateless, h2 frameOptions 허용 |
| 에러 포맷 유지 | AuthenticationEntryPoint(401) / AccessDeniedHandler(403) → 기존 `{status, message}` JSON |
| `AuthService` | signup: BCrypt encode · login: `matches()` + JWT 발급 · `TokenStore` 삭제 |
| Controller | `@RequestHeader("X-AUTH-TOKEN")` 제거 → `@AuthenticationPrincipal User` 주입 (관리자 검사는 SecurityConfig가 담당, `requireAdmin` 제거) |
| `DataInitializer` | 시드 비밀번호 BCrypt 인코딩 |

의존성 추가: `spring-boot-starter-security`, `io.jsonwebtoken:jjwt-api/impl/jackson`

## 3. 엣지케이스 정의

| # | 케이스 | 기대 동작 |
| :--- | :--- | :--- |
| E1 | 토큰 없음 / 빈 헤더 | 401 `{status, message}` |
| E2 | 위조 서명 토큰 | 401 |
| E3 | 만료 토큰 | 401 |
| E4 | USER 권한으로 /api/admin/** 호출 | 403 |
| E5 | 정지(SUSPENDED)된 계정의 유효 토큰 | 401 (필터에서 차단 — 정지 즉시 접근 불가) |
| E6 | 정지 계정 로그인 | 403 (기존 동작 유지) |
| E7 | 비밀번호 평문 노출 | DB에 BCrypt 해시만 저장 (`$2a$...`) |
| E8 | 콘솔/h2-console 접근 | 인증 없이 접근 가능 (permitAll) |

## 4. 테스트 계획 (QA)

- `JwtProviderTest` (단위): 발급→검증 라운드트립, 만료 토큰 거부, 위조 서명 거부
- `SecurityIntegrationTest` (MockMvc): 무토큰 401, 위조 토큰 401, USER→admin 403,
  로그인→JWT로 보호 API 접근 성공, BCrypt 해시 저장 확인, 정지 계정 토큰 401
- 기존 테스트(페이징/트랜잭션) 회귀 통과
- curl 실측: 전 시나리오 + 콘솔 로그인 동작

## 5. 산출물

- 코드: SecurityConfig, JwtProvider/Filter, BCrypt 적용, TokenStore 제거, 컨트롤러 principal 주입
- 문서: 본 계획 문서, `phase3_3_security_report.md`(완료·엣지케이스·QA),
  `user_mode.md`/`admin_mode.md`(인증 방식·에러 명세), README 기술스택 갱신
