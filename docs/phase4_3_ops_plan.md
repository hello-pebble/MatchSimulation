# Phase 4-3 시작 전 계획 문서 — 캐싱 + 만료 스케줄러 (운영 자동화)

작성일: 2026-08-01
선행 단계: Phase 4-2 (Swagger/OpenAPI) 완료

## 1. 배경과 목표

- 매칭 통계(`/api/admin/stats/matches`)는 전체 매칭·회원을 훑는 집계 연산인데
  호출마다 재계산된다 → **Caffeine 캐시**(TTL 60초) + 매칭 변경 시 **즉시 무효화**
- 응답 없는 매칭 요청(REQUESTED)이 영원히 남는다 → **스케줄러**가 7일 경과 건을
  `EXPIRED`로 자동 만료하고 요청자에게 알림 (백그라운드 배치의 전통 패턴)
- `MatchStatus`에 `EXPIRED` 추가는 **Flyway V2 마이그레이션**으로 스키마 변경 이력을 남긴다
  (V1 이후 스키마 진화를 보여주는 실전 사례)

## 2. 설계

| 구성요소 | 내용 |
| :--- | :--- |
| 캐시 | `spring-boot-starter-cache` + Caffeine. `spring.cache.caffeine.spec=expireAfterWrite=60s` |
| `AdminStatsService.matchStats` | `@Cacheable("matchStats")` |
| 무효화 | `MatchingService.request/respond` + 만료 배치에 `@CacheEvict("matchStats", allEntries=true)` |
| `MatchStatus.EXPIRED` | enum 추가 + `V2__add_expired_status.sql` (H2 enum 컬럼 확장) |
| `MatchExpiryScheduler` | `@Scheduled`(1시간 주기) → 7일 경과 REQUESTED를 EXPIRED로 전이 + 요청자 알림(트랜잭션) |
| 활성화 | `@EnableCaching`, `@EnableScheduling` |

## 3. 엣지케이스 정의

| # | 케이스 | 기대 동작 |
| :--- | :--- | :--- |
| E1 | 통계 연속 조회 | 첫 조회 후 캐시 적중 (DB 변경 직접 반영 안 됨 — TTL 60초) |
| E2 | 매칭 요청/응답 발생 | 캐시 즉시 무효화 → 다음 조회는 최신 통계 |
| E3 | 7일 경과 REQUESTED | EXPIRED 전이 + 요청자 알림 생성 (같은 트랜잭션) |
| E4 | 7일 미만 REQUESTED / 이미 처리된 매칭 | 변경 없음 |
| E5 | 만료된 매칭에 뒤늦은 응답 | 400 "이미 처리된 매칭" (기존 로직이 커버) |
| E6 | V2 마이그레이션 | flyway_schema_history에 V2 기록, EXPIRED 저장 가능 |

## 4. 테스트 계획 (QA)

- `CacheIntegrationTest`: 캐시 적중(직접 DB 변경이 반영되지 않음) → respond 후 무효화되어 최신 반영
- `MatchExpirySchedulerTest`: 8일 전 REQUESTED 생성 → 배치 실행 → EXPIRED + 알림 / 최근 건 유지
- Flyway V2 이력 검증(기존 FlywayMigrationTest 확장), 전체 회귀
- curl 실측: 통계 캐싱 전후 값, 만료 배치 로그

## 5. 산출물

- 코드: 캐시 설정, EXPIRED + V2 SQL, 스케줄러, evict 지점
- 문서: 본 계획, `phase4_3_ops_report.md`, user_mode/admin_mode(EXPIRED·만료 정책), README
