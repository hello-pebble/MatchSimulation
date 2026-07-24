# MatchSimulation

매칭(소개팅) 서비스 백엔드 + 관리자(Admin) 모드 프로젝트입니다.
Java 21 / Spring Boot 4.x / H2 In-Memory DB 기반으로 로컬에서 즉시 실행·테스트할 수 있으며,
외부 AI 매칭 모델·외부 서버를 설정만으로 연동할 수 있는 구조로 설계되었습니다.

## 1. 프로젝트 개요

| 항목 | 내용 |
| :--- | :--- |
| 프로젝트명 | MatchSimulation |
| 개발 단계 | Phase 2 (기술스택 업그레이드 + 관리자 모드 구현 완료) |
| 아키텍처 | 계층형(Layered) 아키텍처 + 매칭 엔진 Interface/Adapter 확장 구조 |

## 2. 기술 스택

| 구분 | 기술 |
| :--- | :--- |
| 언어 | Java 21 (Record DTO, Virtual Threads 활성화) |
| 프레임워크 | Spring Boot 4.1.0 (Spring Framework 7) |
| 저장소 | H2 In-Memory DB + Spring Data JPA (기동 시 시드 데이터 자동 적재) |
| 빌드 도구 | Gradle 9 (Wrapper) |
| 주요 라이브러리 | spring-boot-starter-webmvc, restclient, validation, Lombok, JUnit 5 |
| 인증 | In-Memory 더미 토큰 (`X-AUTH-TOKEN` 헤더) |

## 3. 주요 기능

| 분류 | 상세 |
| :--- | :--- |
| 회원 | 회원가입(PENDING) / 로그인(더미 토큰) / 내 정보 |
| 매칭 | 규칙 기반 추천(지역·나이·직군 점수), 매칭 요청/수락/거절, 내 매칭 이력 |
| Q&A | 문의 등록, 내 문의 조회 |
| 알림 | 전체 공지 + 개별 알림 조회 |
| **관리자** | 회원 목록/상태 변경(승인·정지), Q&A 답변, 알림 등록(전체/개별), 일별·성별·상태별 매칭 통계 |
| 확장성 | `MatchingEngine` 인터페이스 — `matching.engine=external-ai` 설정만으로 외부 AI 서버 연동 |

## 4. 빠른 시작

```bash
./gradlew bootRun     # http://localhost:8080
./gradlew test        # 단위 + 통합 테스트
```

| 접속 주소 | 설명 |
| :--- | :--- |
| /index.html | 회원 테스트 콘솔 |
| /admin.html | 관리자 콘솔 (통계 뷰어 포함) |
| /h2-console | H2 DB 콘솔 (`jdbc:h2:mem:matchdb`, user `sa`) |

샘플 계정: 관리자 `admin@match.com` / `admin1234`, 회원 `male1~10`·`female1~10@match.com` / `pass1234`

## 5. 문서

| 문서 | 내용 |
| :--- | :--- |
| [docs/architecture.md](docs/architecture.md) | 아키텍처 설계서 (패키지 구조, 컴포넌트 역할, AI 연동 접점) |
| [docs/functional_spec.md](docs/functional_spec.md) | 기능 명세서 |
| [docs/api_spec.md](docs/api_spec.md) | API 명세서 (Endpoints, Request/Response) |
| [docs/local_guide.md](docs/local_guide.md) | 로컬 실행 및 테스트 가이드 |

## 6. 향후 개발 계획

| 단계 | 주요 작업 |
| :--- | :--- |
| Phase 3 | 외부 AI 매칭 서버 연동 (`external-ai` 엔진 활성화), LLM 기반 프로필 분석 |
| Phase 4 | 외부 RDBMS 전환, JWT 인증, 실시간 채팅(WebSocket) |
