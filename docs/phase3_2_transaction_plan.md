# Phase 3-2 시작 전 계획 문서 — 트랜잭션 경계 & 낙관적 락

작성일: 2026-07-30
선행 단계: Phase 3-1 (페이징/정렬) 완료

## 1. 배경과 목표

매칭 응답(수락/거절)은 "매칭 상태 변경"과 "알림 생성"이라는 두 쓰기 작업을
수반한다. 지금은 상태 변경만 트랜잭션 안에 있고 알림은 생성하지 않는다.
전통 백엔드의 핵심 요소인 **트랜잭션 경계(원자성)**와 **동시성 제어(낙관적 락)**를
명시적으로 도입한다.

- 수락 시: 상태 변경 + **양측(요청자/수락자) 알림 생성**을 하나의 트랜잭션으로 —
  알림 저장이 실패하면 상태 변경도 **롤백**
- 동시 응답 경쟁(두 요청이 같은 매칭에 동시에 수락/거절): **낙관적 락**으로
  한쪽만 성공, 다른 쪽은 409 CONFLICT
- 관리자 상태 변경도 동일 패턴: 승인/정지 시 대상 회원 알림을 같은 트랜잭션으로 생성

## 2. 설계

| 변경 | 내용 |
| :--- | :--- |
| `MatchRecord` | `@Version Long version` 추가 (JPA 낙관적 락) |
| `NotificationService` | 내부용 `notify(targetUserId, title, message)` 추가 — 서비스 간 재사용 |
| `MatchingService.respond` | `@Transactional` 내에서 상태 전이 + 수락 시 양측 알림 생성 |
| `UserService.changeStatus` | `@Transactional` 내에서 상태 변경 + 승인/정지 알림 생성 |
| `GlobalExceptionHandler` | `OptimisticLockingFailureException` → **409** `{status, message}` |

알림 문구:
- 수락 → 요청자: "매칭이 성사되었습니다!" / 수락자: "매칭 수락을 완료했습니다"
- 승인(ACTIVE): "회원 승인이 완료되었습니다" / 정지(SUSPENDED): "계정이 정지되었습니다"

## 3. 엣지케이스 정의

| # | 케이스 | 기대 동작 |
| :--- | :--- | :--- |
| E1 | 동시에 같은 매칭에 두 번 응답 (경쟁) | 1건 성공, 1건 409(낙관적 락) 또는 400(이미 처리) — 상태는 정확히 1회만 전이 |
| E2 | 알림 저장 실패 | 매칭 상태 변경까지 **롤백** (REQUESTED 유지) |
| E3 | 이미 처리된 매칭 재응답 | 400 (기존 동작 유지) |
| E4 | 거절(REJECTED) 시 | 알림 미생성(수락만 알림), 정상 커밋 |
| E5 | 관리자 정지 시 알림 실패 | 상태 변경 롤백 |
| E6 | 탈퇴 회원 이름 표시 | "(탈퇴 회원)" 기존 동작 유지 |

## 4. 테스트 계획 (QA)

- `TransactionIntegrationTest`
  - 수락 시 양측 알림 2건 생성 + 상태 ACCEPTED 검증
  - 동시성: ExecutorService 2스레드 동시 respond → 정확히 1건 성공
  - 롤백: NotificationService 목(mock)이 예외 던지면 상태 REQUESTED 유지 (`@MockitoBean`)
  - 거절 시 알림 미생성
- curl 실측: 수락 후 양측 `/api/notifications/my`에 알림 확인, 관리자 정지 후 대상 알림 확인

## 5. 산출물

- 코드: @Version, notify(), respond/changeStatus 트랜잭션 확장, 409 매핑
- 문서: 본 계획 문서, `phase3_2_transaction_report.md`(완료·엣지케이스·QA),
  `user_mode.md`/`admin_mode.md` API 명세(409 추가, 알림 동작) 갱신
