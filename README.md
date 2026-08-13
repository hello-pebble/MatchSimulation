# AdminCore

운영 관리자 콘솔 백엔드입니다. 회원 관리, Q&A 답변, 알림 발송, 매칭 현황 통계를
하나의 관리자 API/콘솔로 제공합니다.
Java 21 / Spring Boot 4.x / PostgreSQL 기반이며, DB는 로컬 Docker(`docker compose up -d db`)로 띄웁니다.

> 이 저장소는 매칭 서비스 프로젝트(MatchSimulation)의 사용자 모드를 걷어내고
> **관리자 모드만 남겨 고도화**하는 방향으로 재정의되었습니다.
> 전환 이전의 단계별 기록은 `docs/phase*.md`에 그대로 보존되어 있습니다.

## 1. 프로젝트 개요

| 항목 | 내용 |
| :--- | :--- |
| 프로젝트명 | AdminCore |
| 성격 | 운영 관리자 전용 백엔드 (사용자 대면 기능 없음) |
| 아키텍처 | 기능별 모듈(package-by-feature), 관리자 API 단일 진입점 + 공용 도메인 |

## 2. 기술 스택

| 구분 | 기술 |
| :--- | :--- |
| 언어 | Java 21 (Record DTO, Virtual Threads 활성화) |
| 프레임워크 | Spring Boot 4.1.0 (Spring Framework 7) |
| 저장소 | **PostgreSQL 17**(로컬 Docker) + Spring Data JPA + **Flyway 마이그레이션**(`db/migration`, ddl-auto=validate) |
| 빌드 도구 | Gradle 9 (Wrapper) |
| 인증 | Spring Security + **JWT**(HS256, 60분) + BCrypt — `X-AUTH-TOKEN` 헤더 |
| 문서 | springdoc-openapi (Swagger UI) |
| 캐시/배치 | Caffeine(TTL 60초) + `@Scheduled` 만료 배치 |

## 3. 주요 기능

| 분류 | 상세 |
| :--- | :--- |
| 인증/인가 | JWT 로그인, `/api/admin/**`은 `hasRole('ADMIN')` — 미달 시 403 JSON |
| 회원 관리 | 페이징 회원 목록, 상태 변경(`PENDING`/`ACTIVE`/`SUSPENDED`) + 대상 회원 알림 발송(동일 트랜잭션) |
| Q&A 관리 | 전체/상태별 문의 목록(페이징), 답변 등록 → `ANSWERED` |
| 알림 발송 | 전체 공지 또는 개별 회원 알림 생성, 발송 이력 조회 |
| 매칭 통계 | 전체/성사 건수, 성사율, 일별·성별·상태별 집계 — **DB GROUP BY 집계** + 60초 캐시 |
| 운영 배치 | 7일 무응답 매칭 자동 만료(`EXPIRED`) + 요청자 알림 + 통계 캐시 무효화 |

## 4. 모듈 구조 (package-by-feature)

```
com.pebble.admincore
├── common        # 공통 예외/에러 응답, 페이징 정책, Security·Cache·OpenAPI 설정
├── config        # 최초 기동 시 시드 데이터 초기화
├── user          # 계정 도메인 + 인증(JWT) + 회원 상태 변경
├── qna           # 문의 도메인 + 관리자 답변
├── notification  # 알림 도메인 + 발송/이력
├── matching      # 매칭 기록(통계 입력 데이터) + 만료 배치
└── admin         # 관리자 API 단일 진입점 + 통계 집계
```

관리자 API는 `admin` 모듈이 단일 진입점이며, 실제 도메인 로직은 각 기능 모듈의
서비스가 소유합니다. `matching` 모듈은 **쓰기 경로 없이** 통계의 입력 데이터와
만료 배치만 담당합니다.

## 5. API 요약

| 구분 | 메서드/경로 | 설명 |
| :--- | :--- | :--- |
| 인증 | POST /api/auth/signup, /login · GET /api/auth/me | 가입(PENDING) / 로그인(JWT) / 내 정보 |
| 회원 관리 | GET /api/admin/users · PATCH /api/admin/users/{id}/status | 목록(페이징) / 승인·정지 |
| Q&A | GET /api/admin/qna · POST /api/admin/qna/{id}/answer | 목록(상태 필터·페이징) / 답변 |
| 알림 | GET·POST /api/admin/notifications | 발송 이력 / 공지·개별 알림 발송 |
| 통계 | GET /api/admin/stats/matches | 전체·성사·성사율 + 일별/성별/상태별 |

보호 API는 로그인 응답의 JWT를 `X-AUTH-TOKEN` 헤더로 전달합니다.
상세 명세는 [docs/admin_mode.md](docs/admin_mode.md) 참조.

## 6. 빠른 시작

```bash
docker compose up -d db   # PostgreSQL 기동 (localhost:5432)
./gradlew bootRun         # http://localhost:8080
./gradlew test            # 단위 + 통합 테스트 (DB 없이도 실행됨)
```

앱까지 컨테이너로 띄우려면 `docker compose --profile app up`.
DB 접속 정보는 `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` 환경변수로 덮어쓸 수 있습니다.

| 접속 주소 | 설명 |
| :--- | :--- |
| /admin.html | 관리자 콘솔 (통계 뷰어 포함) |
| /swagger-ui.html | Swagger API 문서 (Authorize에 JWT 입력 후 실호출 가능) |


샘플 계정: 관리자 `admin@match.com` / `admin1234`

## 7. 문서

| 문서 | 내용 |
| :--- | :--- |
| [docs/admin_mode.md](docs/admin_mode.md) | 관리자 모드 문서 (기능 명세 + API + 콘솔 시나리오) |
| [docs/architecture.md](docs/architecture.md) | 아키텍처 설계서 (모듈 구조, 통계 집계, 인증 흐름, ERD) |
| [docs/usecase.md](docs/usecase.md) | 유즈케이스 문서 |
| [docs/local_guide.md](docs/local_guide.md) | 로컬 실행 및 테스트 가이드 |
| docs/phase*.md | 관리자 전용 전환 이전(MatchSimulation)의 단계별 계획/보고 기록 |

## 8. 향후 개발 계획

| 단계 | 주요 작업 |
| :--- | :--- |
| 운영 | 관리자 액션 감사 로그, 권한 세분화(ADMIN 단일 롤 → 역할 분리) |
| 조회 | 회원/문의 검색·필터, 통계 CSV export |
| 관측성 | Actuator/메트릭, 통계 쿼리 실행계획 모니터링 |
