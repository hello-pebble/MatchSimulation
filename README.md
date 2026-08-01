# MatchSimulation

매칭(소개팅) 서비스 백엔드 + 관리자(Admin) 모드 프로젝트입니다.
Java 21 / Spring Boot 4.x / H2 In-Memory DB 기반으로 로컬에서 즉시 실행·테스트할 수 있으며,
외부 AI 매칭 모델·외부 서버를 설정만으로 연동할 수 있는 구조로 설계되었습니다.

## 1. 프로젝트 개요

| 항목 | 내용 |
| :--- | :--- |
| 프로젝트명 | MatchSimulation |
| 개발 단계 | Phase 5-3 (1:1 채팅 — 새로고침 → Short Polling → Long Polling 완료) |
| 아키텍처 | 기능별 모듈(package-by-feature) 구조 + 매칭 엔진 Interface/Adapter 확장 구조 |

## 2. 기술 스택

| 구분 | 기술 |
| :--- | :--- |
| 언어 | Java 21 (Record DTO, Virtual Threads 활성화) |
| 프레임워크 | Spring Boot 4.1.0 (Spring Framework 7) |
| 저장소 | H2 In-Memory DB + Spring Data JPA + **Flyway 마이그레이션**(`db/migration`, ddl-auto=validate) |
| 빌드 도구 | Gradle 9 (Wrapper) |
| 주요 라이브러리 | spring-boot-starter-webmvc, restclient, validation, security, Lombok, JUnit 5 |
| 인증 | Spring Security + **JWT**(HS256, 60분) + BCrypt — `X-AUTH-TOKEN` 헤더 |

## 3. 주요 기능

| 분류 | 상세 |
| :--- | :--- |
| 회원 | 회원가입(PENDING) / 로그인(더미 토큰) / 내 정보 |
| 매칭 | 규칙 기반 추천(지역·나이·직군 점수), 매칭 요청/수락/거절, 내 매칭 이력, 7일 무응답 자동 만료 배치 |
| 채팅 | 매칭 성사(ACCEPTED) 상대와 1:1 대화 — afterId 증분 조회, 새로고침/Short Polling/**Long Polling** 3단계 수신 |
| Q&A | 문의 등록, 내 문의 조회 |
| 알림 | 전체 공지 + 개별 알림 조회 |
| **관리자** | 회원 목록/상태 변경(승인·정지), Q&A 답변, 알림 등록(전체/개별), 일별·성별·상태별 매칭 통계 |
| 확장성 | `MatchingEngine` 인터페이스 — `matching.engine=external-ai` 설정만으로 외부 AI 서버 연동 |

## 4. 모듈 구조 (package-by-feature)

각 기능을 독립 모듈로 분리했으며, 모듈 내부는 `controller / service / repository / domain / dto`로 구성됩니다.

```
com.pebble.mvp
├── common        # 공통 예외, JSON 에러 응답
├── config        # H2 시드 데이터 초기화
├── user          # 회원가입 / 로그인(더미 토큰) / 계정
├── matching      # 추천, 매칭 요청/응답, 매칭 엔진(AI 연동 접점: matching.engine)
├── chat          # 매칭 성사 상대와 1:1 채팅 (afterId 증분 조회)
├── qna           # 1:1 문의 (유저 등록, 관리자 답변)
├── notification  # 공지/알림
└── admin         # 관리자 회원관리 / QnA / 알림등록 / 매칭 통계
```

## 5. 주요 API 요약

| 구분 | 메서드/경로 | 설명 |
| :--- | :--- | :--- |
| 인증 | POST /api/auth/signup, /login · GET /api/auth/me | 가입(PENDING) / 로그인(토큰) / 내 정보 |
| 매칭 | GET /api/matching/recommendations · POST /api/matching/requests, /requests/{id}/respond · GET /api/matching/my | 추천 / 요청 / 수락·거절 / 내 매칭 |
| 채팅 | GET /api/chat/rooms · POST·GET /api/chat/{matchId}/messages(?afterId=N) · GET .../messages/poll | 대화방 목록 / 전송 / 증분 조회 / Long Polling |
| QnA | POST /api/qna · GET /api/qna/my | 문의 등록 / 내 문의 |
| 알림 | GET /api/notifications/my | 내 알림(전체 공지 + 개별) |
| 관리자 | GET·PATCH /api/admin/users(/{id}/status) · GET·POST /api/admin/qna(/{id}/answer) · GET·POST /api/admin/notifications · GET /api/admin/stats/matches | 회원관리 / QnA 답변 / 알림 등록 / 매칭 통계 |

인증이 필요한 API는 로그인 응답의 토큰을 `X-AUTH-TOKEN` 헤더로 전달합니다. 상세 명세는 [docs/user_mode.md](docs/user_mode.md), [docs/admin_mode.md](docs/admin_mode.md) 참조.

## 6. 빠른 시작

```bash
./gradlew bootRun     # http://localhost:8080
./gradlew test        # 단위 + 통합 테스트
```

| 접속 주소 | 설명 |
| :--- | :--- |
| /index.html | 회원 테스트 콘솔 |
| /admin.html | 관리자 콘솔 (통계 뷰어 포함) |
| /h2-console | H2 DB 콘솔 (`jdbc:h2:mem:matchdb`, user `sa`) |
| /swagger-ui.html | Swagger API 문서 (Authorize에 JWT 입력 후 실호출 가능) |

샘플 계정: 관리자 `admin@match.com` / `admin1234`, 회원 `male1~10`·`female1~10@match.com` / `pass1234`

## 7. 문서

| 문서 | 내용 |
| :--- | :--- |
| [docs/phase2_plan.md](docs/phase2_plan.md) | Phase 2 시작 전 계획 문서 (목표, 설계 방향) |
| [docs/phase2_report.md](docs/phase2_report.md) | Phase 2 완료 보고 문서 (구현 결과, 검증 내역) |
| [docs/architecture.md](docs/architecture.md) | 아키텍처 설계서 (모듈 구조, 컴포넌트 역할, AI 연동 접점) |
| [docs/usecase.md](docs/usecase.md) | 유즈케이스 문서 (사용자/관리자 모드별 유즈케이스 + 시나리오) |
| [docs/user_mode.md](docs/user_mode.md) | 사용자 모드 문서 (기능 명세 + API + 콘솔 시나리오) |
| [docs/admin_mode.md](docs/admin_mode.md) | 관리자 모드 문서 (기능 명세 + API + AI 연동 계약) |
| [docs/local_guide.md](docs/local_guide.md) | 로컬 실행 및 테스트 가이드 |

## 8. 향후 개발 계획

| 단계 | 주요 작업 |
| :--- | :--- |
| 이후 | 실시간 채팅(WebSocket), 외부 RDBMS 전환, 외부 AI 매칭 서버 실연동 |
