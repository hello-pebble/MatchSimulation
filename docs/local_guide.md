# 로컬 실행 및 테스트 가이드

## 1. 요구 사항

| 항목 | 버전 |
| :--- | :--- |
| JDK | 21 이상 |
| Gradle | Wrapper 포함 (별도 설치 불필요, Gradle 9.0) |

## 2. 실행

```bash
./gradlew bootRun          # (Windows: gradlew.bat bootRun)
```

또는 빌드 후 jar 실행:

```bash
./gradlew build
java -jar build/libs/AdminHub-1.0-SNAPSHOT.jar
```

기동하면 Flyway가 스키마를 만들고(V1·V2·V4), H2 In-Memory DB에 시드 데이터
(관리자 1명 + 회원 20명 + 매칭 30건 + 문의 3건 + 알림 2건)가 자동 적재됩니다.
재기동 시 초기화됩니다.

## 3. 접속 주소

| 주소 | 설명 |
| :--- | :--- |
| http://localhost:8080/admin.html | 관리자 콘솔 (통계 뷰어 포함) |
| http://localhost:8080/swagger-ui.html | Swagger API 문서 (Authorize에 JWT 입력) |
| http://localhost:8080/h2-console | H2 콘솔 — JDBC URL `jdbc:h2:mem:adminhubdb`, user `sa`, 비밀번호 없음 |

## 4. 샘플 계정

| 구분 | 이메일 | 비밀번호 | 상태 |
| :--- | :--- | :--- | :--- |
| 관리자 | admin@match.com | admin1234 | ACTIVE |
| 일반 회원 | male1~10@match.com / female1~10@match.com | pass1234 | 1~8번 ACTIVE, 9번 PENDING, 10번 SUSPENDED |

일반 회원 계정은 관리자 기능의 대상 데이터이자 403 인가 테스트용입니다.

## 5. 시나리오 테스트 (관리자 콘솔 기준)

1. `/admin.html`에서 `admin@match.com` / `admin1234`로 **로그인**
2. **회원 조회** → PENDING 회원의 userId로 **상태 변경(승인)** → 대상 회원 알림 생성 확인
3. **답변 대기** 조회 → qnaId 입력 후 **답변 등록** → `ANSWERED` 전환 확인
4. **알림 생성** — 대상 userId를 비우면 전체 공지, 지정하면 개별 알림
5. **통계 새로고침** → 전체/성사/성사율 카드와 일별·성별·상태별 막대그래프 확인
6. 일반 회원 계정으로 로그인해 `/api/admin/users` 호출 → **403** 확인

## 6. curl 예시

```bash
# 로그인 → 토큰 추출
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@match.com","password":"admin1234"}' | jq -r .token)

# 전체 회원 조회 (페이징)
curl -s "localhost:8080/api/admin/users?page=0&size=10&sort=createdAt,desc" \
  -H "X-AUTH-TOKEN: $TOKEN" | jq

# 답변 대기 문의
curl -s "localhost:8080/api/admin/qna?status=WAITING" -H "X-AUTH-TOKEN: $TOKEN" | jq

# 매칭 통계
curl -s localhost:8080/api/admin/stats/matches -H "X-AUTH-TOKEN: $TOKEN" | jq
```

## 7. 자동화 테스트

```bash
./gradlew test
```

| 테스트 | 검증 내용 |
| :--- | :--- |
| `AdminHubApplicationTests` | 컨텍스트 로드 + 시드 데이터 + 통계 집계 |
| `AdminStatsAggregationTest` | DB GROUP BY 집계 결과가 독립 계산한 기대값과 일치 |
| `CacheIntegrationTest` | 통계 캐시 적중 + 만료 배치 시 무효화 |
| `PagingIntegrationTest` | 페이징/정렬/size 클램프/잘못된 정렬 필드 400 |
| `SecurityIntegrationTest` | 401/403, JWT 인증, BCrypt 저장, 정지 계정 토큰 무효화 |
| `UserStatusTransactionTest` | 상태 변경 + 알림 생성의 트랜잭션 결합 |
| `UserStatusRollbackTest` | 알림 저장 실패 시 상태 변경 롤백 |
| `FlywayMigrationTest` | 마이그레이션 이력 + 통계 인덱스 생성 확인 |
| `MatchExpirySchedulerTest` | 7일 경과 REQUESTED만 EXPIRED 전이 + 요청자 알림 |
| `OpenApiIntegrationTest` | OpenAPI 문서에 관리자 경로 노출 / 사용자 모드 경로 부재 |
| `JwtProviderTest` | JWT 발급·검증 단위 테스트 |

## 8. 통계 쿼리 확인 (선택)

집계가 실제로 DB에서 수행되는지 보려면 SQL 로그를 켭니다.

```yaml
# application.yml
logging:
  level:
    org.hibernate.SQL: debug
```

`GET /api/admin/stats/matches` 호출 시 `group by`가 포함된 3개 쿼리만 나가고,
60초 이내 재호출은 캐시 적중으로 쿼리가 나가지 않아야 합니다.
실행계획은 H2 콘솔에서 `explain analyze select ...`로 확인할 수 있습니다.
