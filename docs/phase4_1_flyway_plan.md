# Phase 4-1 시작 전 계획 문서 — Flyway 스키마 마이그레이션

작성일: 2026-07-31
선행 단계: Phase 3 (페이징 / 트랜잭션·락 / Security·JWT) 완료

## 1. 배경과 목표

현재 스키마는 `ddl-auto: create-drop`으로 Hibernate가 매 기동 시 생성한다.
이 방식은 스키마 변경 이력이 남지 않고, 운영 DB에는 쓸 수 없다.
전통 백엔드의 표준 관행인 **DB 마이그레이션 도구(Flyway)** 를 도입해
"스키마를 코드로 버전 관리"하는 구조로 전환한다.

- 스키마 소유권: Hibernate → **Flyway** (`V1__init.sql`)
- Hibernate는 `ddl-auto: validate` — 엔티티와 스키마 불일치 시 기동 실패(안전장치)
- 데이터 시드(`DataInitializer`)는 앱 레이어 유지 (데이터는 마이그레이션 대상 아님)
- H2 in-memory 특성상 매 기동 시 V1부터 재적용 — 외부 RDBMS 전환 시에도 동일 파일 사용

## 2. 설계

| 변경 | 내용 |
| :--- | :--- |
| 의존성 | `spring-boot-starter-flyway` (Boot 4 신규 스타터, BOM 버전 관리) |
| `db/migration/V1__init.sql` | Hibernate가 생성하던 DDL을 실측 덤프하여 명시적 SQL로 고정 (users, match_records(version 포함), qna, notifications) |
| `application.yml` | `ddl-auto: create-drop` → `validate` |

## 3. 엣지케이스 정의

| # | 케이스 | 기대 동작 |
| :--- | :--- | :--- |
| E1 | 엔티티-DDL 불일치 (컬럼 누락 등) | `validate`가 기동 실패로 차단 (의도된 안전장치) |
| E2 | 마이그레이션 파일 변경(체크섬 불일치) | H2 인메모리는 매 기동 초기화라 미발생 — 외부 DB 전환 시 Flyway가 감지(문서화) |
| E3 | flyway_schema_history 기록 | V1 적용 이력 1건, success=true |
| E4 | 시드 데이터 정상 적재 | 기존과 동일 (users=21, matches=30, qna=3, notifications=2) |

## 4. 테스트 계획 (QA)

- 기존 전체 테스트(28건) 회귀 — validate 모드에서 전부 통과하는 것 자체가 스키마 일치 증명
- 신규 `FlywayMigrationTest`: flyway_schema_history에 V1 성공 기록 검증
- curl/H2 콘솔 실측: 기동 로그의 Flyway 적용 메시지, API 정상 동작

## 5. 산출물

- 코드: V1__init.sql, build.gradle, application.yml
- 문서: 본 계획 문서, `phase4_1_flyway_report.md`, README 기술스택 갱신
