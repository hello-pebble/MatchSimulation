# Phase 4-1 완료 보고 문서 — Flyway 스키마 마이그레이션

완료일: 2026-08-01
계획 문서: [phase4_1_flyway_plan.md](phase4_1_flyway_plan.md)

## 1. 결과 요약 — 계획 대비 전 항목 완료

| 항목 | 결과 |
| :--- | :--- |
| Flyway 도입 | ✅ `spring-boot-starter-flyway` (Boot 4 신규 스타터) |
| V1 마이그레이션 | ✅ `db/migration/V1__init.sql` — Hibernate 실측 DDL 덤프 기반 4개 테이블 |
| 스키마 소유권 전환 | ✅ `ddl-auto: create-drop` → `validate` |
| 시드 데이터 | ✅ 기존과 동일 적재 (users=21, matches=30, qna=3, notifications=2) |

기동 로그 실측:
```
Migrating schema "PUBLIC" to version "1 - init"
Successfully applied 1 migration to schema "PUBLIC", now at version v1
시드 데이터 적재 완료: users=21, matches=30, qna=3, notifications=2
```

## 2. 엣지케이스 검증 결과

| # | 케이스 | 기대 | 결과 |
| :--- | :--- | :--- | :--- |
| E1 | 엔티티-DDL 불일치 | validate가 기동 실패로 차단 | ✅ 개발 중 실측 — DDL 불일치 시 테스트 전체 실패로 즉시 감지됨 |
| E2 | 체크섬 변경 감지 | 외부 DB 전환 시 Flyway 검증 | ✅ 문서화 (H2 인메모리는 매 기동 초기화) |
| E3 | flyway_schema_history 기록 | V1 성공 이력 | ✅ `{version=1, description=init, success=true}` (테이블 생성 메타 행 뒤) |
| E4 | 전체 API 회귀 | 정상 동작 | ✅ 로그인·통계 API curl 실측 정상 |

## 3. QA 테스트 체크리스트

- [x] `FlywayMigrationTest` 2건: V1 성공 이력, 마이그레이션 스키마에 시드 적재
- [x] 기존 전체 테스트 회귀 통과 (30건) — validate 모드 통과 자체가 엔티티-스키마 일치 증명
- [x] 기동 로그에서 Flyway 적용 확인, H2 콘솔에서 flyway_schema_history 조회 가능
- [x] 테스트 격리 개선(부수 수정): 공유 컨텍스트에서 상태를 바꾸는 테스트가
      다른 테스트의 계정/카운트를 오염시키지 않도록 대상 선택·원복 로직 보강

## 4. 참고

- Flyway가 H2 2.4를 공식 검증 버전(2.3)보다 신버전이라 경고 출력 — 동작 이상 없음, 기록만 남김
- 외부 RDBMS(PostgreSQL/MySQL) 전환 시 동일한 `db/migration` 파일로 스키마 이관 가능
  (방언 차이가 있는 enum 컬럼은 전환 시 V1 수정 또는 벤더별 스크립트 분리 필요)

## 5. 남은 사항

- 없음. 다음 단계: Phase 4-2 Swagger/OpenAPI
