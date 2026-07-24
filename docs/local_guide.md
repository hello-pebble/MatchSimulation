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
java -jar build/libs/MatchSimulation-1.0-SNAPSHOT.jar
```

기동하면 H2 In-Memory DB에 시드 데이터(관리자 1명 + 회원 20명 + 매칭 30건 +
문의 3건 + 알림 2건)가 자동 적재됩니다. 재기동 시 초기화됩니다.

## 3. 접속 주소

| 주소 | 설명 |
| :--- | :--- |
| http://localhost:8080/index.html | 회원 테스트 콘솔 |
| http://localhost:8080/admin.html | 관리자 테스트 콘솔 (통계 뷰어 포함) |
| http://localhost:8080/h2-console | H2 콘솔 — JDBC URL `jdbc:h2:mem:matchdb`, user `sa`, 비밀번호 없음 |

## 4. 샘플 계정

| 구분 | 이메일 | 비밀번호 | 상태 |
| :--- | :--- | :--- | :--- |
| 관리자 | admin@match.com | admin1234 | ACTIVE |
| 일반 회원 | male1~10@match.com / female1~10@match.com | pass1234 | 1~8번 ACTIVE, 9번 PENDING, 10번 SUSPENDED |

## 5. 시나리오 테스트 (UI 기준)

1. **회원 콘솔**(`/index.html`)에서 `male1@match.com`으로 로그인 → **추천 받기**
2. 추천 목록의 userId로 **매칭 요청**
3. 상대 계정(예: `female1@match.com`)으로 로그인 → **내 매칭 목록**에서 matchId 확인 → **수락/거절**
4. **문의 등록** 후 **관리자 콘솔**(`/admin.html`)에서 로그인 → **답변 대기** 조회 → 답변 등록
5. 관리자 콘솔에서 **알림 생성**(대상 비우면 전체 공지) → 회원 콘솔 **내 알림** 확인
6. 관리자 콘솔 **통계 새로고침** → 일별/성별/상태별 막대그래프 확인
7. 회원 콘솔에서 신규 **회원가입**(PENDING) → 관리자 **상태 변경**으로 ACTIVE 승인

## 6. curl 예시

```bash
# 로그인 → 토큰 추출
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@match.com","password":"admin1234"}' | jq -r .token)

# 전체 회원 조회
curl -s localhost:8080/api/admin/users -H "X-AUTH-TOKEN: $TOKEN" | jq

# 매칭 통계
curl -s localhost:8080/api/admin/stats/matches -H "X-AUTH-TOKEN: $TOKEN" | jq
```

## 7. 자동화 테스트

```bash
./gradlew test
```

- `LocalMatchingEngineTest` — 규칙 기반 매칭 점수/정렬 단위 테스트
- `MatchSimulationApplicationTests` — 컨텍스트 로드 + 시드 데이터 + 통계 집계 통합 테스트

## 8. 외부 AI 매칭 서버 연동 (선택)

`src/main/resources/application.yml`:

```yaml
matching:
  engine: external-ai              # local → external-ai 변경
  ai:
    base-url: http://your-ai-server:9090
```

재기동하면 추천 API가 외부 서버(`POST /api/v1/recommend`)를 호출합니다.
요청/응답 계약은 `docs/api_spec.md` 5장 참조.
