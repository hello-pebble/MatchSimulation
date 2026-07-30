# Phase 3-2 완료 보고 문서 — 트랜잭션 경계 & 낙관적 락

완료일: 2026-07-30
계획 문서: [phase3_2_transaction_plan.md](phase3_2_transaction_plan.md)

## 1. 결과 요약 — 계획 대비 전 항목 완료

| 항목 | 결과 |
| :--- | :--- |
| `MatchRecord` 낙관적 락 | ✅ `@Version Long version` 추가 |
| 수락 시 상태 전이 + 양측 알림 원자성 | ✅ `MatchingService.respond` 단일 `@Transactional` |
| 알림 실패 시 롤백 | ✅ 테스트로 증명 (상태 REQUESTED 유지) |
| 동시 응답 경쟁 차단 | ✅ 1건만 성공, 나머지 409/400 |
| 관리자 승인/정지 알림 트랜잭션 | ✅ `UserService.changeStatus` 확장 |
| 409 CONFLICT 매핑 | ✅ `OptimisticLockingFailureException` → `{status:409, message}` |
| 알림 재사용 접점 | ✅ `NotificationService.notify()` (호출자 트랜잭션 참여) |

## 2. 엣지케이스 검증 결과

| # | 케이스 | 기대 | 결과 |
| :--- | :--- | :--- | :--- |
| E1 | 2스레드 동시 respond | 정확히 1건 성공 | ✅ 자동 테스트 (success=1, failure=1) |
| E2 | 알림 저장 실패 | 상태 변경 롤백 | ✅ `TransactionRollbackTest` (REQUESTED 유지) |
| E3 | 이미 처리된 매칭 재응답 | 400 | ✅ curl 실측 `"이미 처리된 매칭입니다. 상태: ACCEPTED"` |
| E4 | 거절(REJECTED) | 알림 미생성 | ✅ 자동 테스트 (count 불변) |
| E5 | 정지 시 알림 | 같은 트랜잭션 생성 | ✅ 자동 테스트 + curl 실측 |
| E6 | 탈퇴 회원 이름 | "(탈퇴 회원)" 유지 | ✅ 기존 로직 무변경 |

## 3. QA 테스트 체크리스트

### 자동 (5/5 통과)
- [x] 수락 → ACCEPTED + 알림 2건(요청자 "매칭이 성사되었습니다!", 수락자 "매칭 수락 완료")
- [x] 거절 → REJECTED + 알림 0건
- [x] 동시 응답 2스레드 → 1 성공 / 1 실패, 상태는 1회만 전이
- [x] 알림 실패 시 상태 롤백 (`@MockitoBean`으로 예외 주입)
- [x] 관리자 정지 → 대상 회원 "계정 정지 안내" 알림

### 수동 (2026-07-30 curl 실측)
- [x] 요청 → 수락 → 요청자 알림 목록에 "매칭이 성사되었습니다!" 확인
- [x] 수락자 알림 목록에 "매칭 수락 완료" 확인
- [x] 재응답 400, 정지 후 로그인 403("정지된 계정입니다") 확인
- [x] `./gradlew build` 전체 테스트 통과 (16건)

## 4. API 명세 변경 (요약)

- `POST /api/matching/requests/{matchId}/respond`
  - **409 추가**: 동시 응답 경쟁에서 밀린 요청 →
    `{"status":409,"message":"다른 요청이 먼저 처리되었습니다. 새로고침 후 다시 시도하세요."}`
  - 수락 성공 시 부수효과: 양측에 알림 생성 (`GET /api/notifications/my`로 확인)
- `PATCH /api/admin/users/{userId}/status`
  - 부수효과: ACTIVE 변경 시 "회원 승인 완료", SUSPENDED 변경 시 "계정 정지 안내" 알림 생성

## 5. 남은 사항

- 없음. 다음 단계: Phase 3-3 Spring Security + JWT + BCrypt
