# Phase 4-3 완료 보고 문서 — 캐싱 + 만료 스케줄러

완료일: 2026-08-01
계획 문서: [phase4_3_ops_plan.md](phase4_3_ops_plan.md)

## 1. 결과 요약 — 계획 대비 전 항목 완료

| 항목 | 결과 |
| :--- | :--- |
| Caffeine 캐시 | ✅ `matchStats` 캐시, TTL 60초 (`spring.cache.caffeine.spec`) |
| 캐시 무효화 | ✅ 매칭 요청/응답/만료 배치에 `@CacheEvict(allEntries)` |
| EXPIRED 상태 | ✅ `MatchStatus.EXPIRED` + **Flyway V2** (`V2__add_expired_status.sql`) |
| 만료 스케줄러 | ✅ `MatchExpiryScheduler` — 1시간 주기, 7일 경과 REQUESTED → EXPIRED + 요청자 알림(트랜잭션) |
| 활성화 | ✅ `@EnableCaching`, `@EnableScheduling` (`CacheConfig`) |

기동 로그 실측: `Successfully applied 2 migrations ... now at version v2`

## 2. 엣지케이스 검증 결과 (2026-08-01 실측)

| # | 케이스 | 기대 | 결과 |
| :--- | :--- | :--- | :--- |
| E1 | 통계 연속 조회 | 캐시 적중 | ✅ 응답시간 0.113s → **0.009s**, 직접 DB 변경 미반영(테스트) |
| E2 | 매칭 요청 발생 | 즉시 무효화 | ✅ 요청 직후 totalMatches 30 → 31 반영 |
| E3 | 8일 경과 REQUESTED | EXPIRED + 알림 | ✅ 자동 테스트 (알림 "매칭 요청 만료") |
| E4 | 1일 경과 REQUESTED / ACCEPTED 30일 | 변경 없음 | ✅ 자동 테스트 |
| E5 | 만료 매칭에 뒤늦은 응답 | 400 | ✅ 기존 "이미 처리된 매칭" 로직이 커버 |
| E6 | V2 마이그레이션 | 이력 기록 | ✅ v1→v2 순차 적용 로그 확인 |

## 3. QA 테스트 체크리스트

- [x] `CacheIntegrationTest`: 캐시 적중(우회 변경 미반영) → 서비스 경유 변경 시 evict로 최신 반영
- [x] `MatchExpirySchedulerTest`: 7일 경과 건만 만료 + 알림, 최근/처리 건 유지
- [x] 전체 34건 회귀 통과 (테스트가 생성한 데이터는 정리하여 공유 컨텍스트 보호)
- [x] curl 실측: 캐시 응답시간, evict 반영, V1+V2 마이그레이션 로그

## 4. API/동작 명세 변경

- 매칭 상태에 `EXPIRED` 추가 — 7일 무응답 요청은 배치가 자동 만료하고 요청자에게 알림
- `/api/admin/stats/matches` 는 최대 60초 캐시 — 단, 매칭 변경 시 즉시 갱신
- 통계 byStatus에 EXPIRED 항목 자연 반영

## 5. Phase 4 (운영성 번들) 전체 완료

| 단계 | 내용 | PR |
| :--- | :--- | :--- |
| 4-1 | Flyway 스키마 마이그레이션 (V1) | #8 |
| 4-2 | Swagger/OpenAPI 자동 문서화 | #9 |
| 4-3 | Caffeine 캐싱 + 만료 스케줄러 + Flyway V2 | 본 단계 |
