# 기능 명세서 (Functional Specification)

작성일: 2026-07-24 (구현 완료 후)
버전: Java 21 / Spring Boot 4.1.0 / H2 In-Memory DB

## 1. 회원(User) 기능

### 1.1 회원가입
- 이메일/비밀번호/이름/나이/성별/직군/지역 입력으로 가입
- 이메일 중복 시 400 에러
- 가입 직후 상태는 `PENDING` (관리자 승인 후 매칭 이용 가능)
- 나이는 19~100세 검증

### 1.2 로그인 (In-Memory 더미 인증)
- 이메일 + 비밀번호 일치 시 UUID 토큰 발급 (`TokenStore`: token → userId, ConcurrentHashMap)
- 이후 모든 인증 API는 `X-AUTH-TOKEN` 헤더로 호출
- `SUSPENDED` 계정은 로그인 차단 (403)
- 비밀번호 불일치 시 401

### 1.3 매칭
- **추천**: `ACTIVE` 상태의 이성 회원을 후보로 `MatchingEngine`이 점수화, 상위 5명 반환
  - 기본 엔진(local): 지역 일치 +40, 나이 차이(1세당 -5, 최대 +40), 직군 일치 +20
  - 각 추천에는 점수 산출 근거(reason) 포함
- **매칭 요청**: 상대 지정 요청 생성 (`REQUESTED`). 자기 자신/비활성 회원/중복 요청 차단
- **매칭 응답**: 요청을 **받은 상대만** 수락(`ACCEPTED`)/거절(`REJECTED`) 가능
- **내 매칭 조회**: 내가 보낸/받은 전체 매칭 이력

### 1.4 Q&A (1:1 문의)
- 문의 등록 (제목 + 내용, 초기 상태 `WAITING`)
- 내 문의 목록 및 관리자 답변 확인

### 1.5 알림
- 전체 공지(targetUserId=null) + 본인 대상 알림을 합쳐 최신순 조회

## 2. 관리자(Admin) 모드

모든 관리자 API는 토큰의 Role이 `ADMIN`이 아니면 403.
기본 관리자 계정: `admin@match.com` / `admin1234`

| 기능 | 내용 |
| :--- | :--- |
| 회원 관리 | 전체 회원 목록 조회, 상태 변경(`PENDING` / `ACTIVE`(승인) / `SUSPENDED`(정지)) |
| Q&A 관리 | 전체/상태별 문의 목록 조회, 답변 작성(→ `ANSWERED`, 답변 시각 기록) |
| 알림 등록 | 전체 공지 또는 개별 회원 대상 알림 생성, 등록된 알림 목록 조회 |
| 매칭 통계 | 전체/성사 건수, 성사율(%), 일별·성별(요청자 기준)·상태별 매칭 건수 요약 |

## 3. 확장성 설계 (구현 결과)

- `MatchingEngine` 인터페이스로 매칭 로직 추상화
  - `LocalMatchingEngine` (기본, `matching.engine=local` 또는 미설정)
  - `ExternalAiMatchingEngine` (`matching.engine=external-ai` 설정 시 `RestClient`로
    `matching.ai.base-url`의 `/api/v1/recommend` 호출) — **코드 수정 없이 설정만으로 교체**
- H2 → 외부 RDBMS 전환: JPA 사용으로 datasource 설정 교체만 필요
- `TokenStore` 단일 클래스 교체로 JWT/Spring Security 전환 가능

## 4. 데이터 (시드)

기동 시 `DataInitializer`가 H2에 자동 적재 (`ddl-auto: create-drop`, 재기동 시 초기화):

| 데이터 | 내용 |
| :--- | :--- |
| 관리자 | admin@match.com / admin1234 |
| 일반 회원 20명 | male1~10, female1~10 @match.com / pass1234 — ACTIVE 16, PENDING 2, SUSPENDED 2 |
| 매칭 기록 30건 | 최근 7일 분산, REQUESTED/ACCEPTED/REJECTED 혼합 |
| 문의 3건 | 답변 대기 2건 + 답변 완료 1건 |
| 알림 2건 | 전체 공지 1건 + 개별 알림 1건 |

## 5. 프론트엔드 (테스트 콘솔)

| 페이지 | 용도 |
| :--- | :--- |
| `/index.html` | 회원 콘솔 — 가입/로그인/추천/매칭 요청·응답/문의/알림 버튼·폼 |
| `/admin.html` | 관리자 콘솔 — 회원관리/Q&A답변/알림등록 + **매칭 통계 막대그래프 뷰어** |
| `/h2-console` | H2 DB 콘솔 (JDBC URL: `jdbc:h2:mem:matchdb`, user: `sa`) |

## 6. 기술 특이사항

- Virtual Threads 활성화 (`spring.threads.virtual.enabled=true`)
- 모든 요청/응답 DTO는 Java 21 **Record**
- 에러는 `GlobalExceptionHandler`가 `{status, message}` JSON으로 통일
- Spring Boot 4 신규 스타터 사용: `spring-boot-starter-webmvc`, `spring-boot-starter-restclient`
