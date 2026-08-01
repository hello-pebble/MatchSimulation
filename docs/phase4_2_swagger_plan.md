# Phase 4-2 시작 전 계획 문서 — Swagger / OpenAPI 자동 문서화

작성일: 2026-08-01
선행 단계: Phase 4-1 (Flyway) 완료

## 1. 배경과 목표

API 명세가 수기 문서(`user_mode.md`/`admin_mode.md`)로만 관리되고 있다.
전통 백엔드 관행인 **코드 기반 API 문서 자동화(springdoc-openapi)** 를 도입해
Swagger UI에서 전체 엔드포인트를 탐색·실호출할 수 있게 한다. 수기 문서는
개념/시나리오 문서로 유지하고, 상세 스펙은 자동 문서가 담당한다.

## 2. 설계

| 항목 | 내용 |
| :--- | :--- |
| 의존성 | `springdoc-openapi-starter-webmvc-ui:3.0.3` (Boot 4 / Framework 7 지원 버전) |
| `common/config/OpenApiConfig` | 서비스 정보 + **SecurityScheme(apiKey, header `X-AUTH-TOKEN`)** 전역 등록 → Swagger UI Authorize 버튼으로 JWT 넣고 실호출 |
| `SecurityConfig` | `/swagger-ui*/**`, `/v3/api-docs*/**` permitAll |
| 컨트롤러 | 모듈별 `@Tag` 최소 부여 (user/matching/qna/notification/admin) |

## 3. 엣지케이스 정의

| # | 케이스 | 기대 동작 |
| :--- | :--- | :--- |
| E1 | 인증 없이 /swagger-ui.html, /v3/api-docs 접근 | 200 (permitAll) |
| E2 | Swagger에서 Authorize 없이 보호 API try-it-out | 401 JSON (기존 계약 유지) |
| E3 | Authorize에 JWT 입력 후 호출 | 정상 응답 |
| E4 | 전체 컨트롤러 경로 노출 | /api/auth, /api/matching, /api/qna, /api/notifications, /api/admin 모두 문서에 존재 |
| E5 | PageResponse 제네릭 스키마 | content 배열 + 페이지 메타 필드 표현 |

## 4. 테스트 계획 (QA)

- `OpenApiIntegrationTest` (MockMvc): `/v3/api-docs` 200, 주요 path 5종 존재,
  securitySchemes에 X-AUTH-TOKEN 정의 존재, swagger-ui 접근 200
- 수동: 브라우저 스크린샷으로 Swagger UI 확인, Authorize→실호출 실측

## 5. 산출물

- 코드: 의존성, OpenApiConfig, SecurityConfig 갱신, @Tag
- 문서: 본 계획, `phase4_2_swagger_report.md`, README 접속 주소에 /swagger-ui.html 추가
