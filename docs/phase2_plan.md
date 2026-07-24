# Phase 2 시작 전 계획 문서 — 기술스택 업그레이드 & 관리자 모드

작성일: 2026-07-24

## 1. 배경

Phase 1은 Java 17 / Spring Boot 3.3.0 / ConcurrentHashMap 인메모리 저장소 기반의
MVP 핵심 로직(교차 매칭)까지 구현된 상태였다. Phase 2에서는 최신 기술스택으로
전환하고, 실제 매칭앱 운영에 필요한 관리자 모드를 백엔드 중심으로 구축한다.

## 2. 목표

| 항목 | 내용 |
| :--- | :--- |
| 기술스택 업그레이드 | Java 17 → **Java 21 (LTS)**, Spring Boot 3.3.0 → **Spring Boot 4.1.0** (Spring Framework 7) |
| 아키텍처 개편 | **기능별 모듈(package-by-feature)** 구조 — user / matching / qna / notification / admin / common |
| 관리자 모드 | 회원관리(승인·정지), QnA 답변, 알림 등록(전체/개별), 매칭 현황 통계 |
| 프론트엔드 | 백엔드 동작 확인용 **버튼 기반 테스트 콘솔** (회원용 index.html + 관리자용 admin.html) |
| 데이터 | H2 In-Memory DB + 시드 데이터 자동 적재 → 로컬에서 별도 설정 없이 즉시 실행 |
| 확장성 | 외부 AI 매칭 서버를 **설정만으로** 연결하는 MatchingEngine 인터페이스/어댑터 구조 |

## 3. 기술스택 변경 상세

| 구분 | 기존 | 변경 | 비고 |
| :--- | :--- | :--- | :--- |
| Java | 17 | 21 | Record DTO, Virtual Threads 활성화 |
| Spring Boot | 3.3.0 | 4.1.0 | Boot 4의 신규 starter 명칭 적용 |
| Web Starter | starter-web | starter-webmvc | Boot 4 명칭 변경 |
| HTTP Client | 없음 | starter-restclient | 외부 AI 서버 연동 대비 |
| 저장소 | ConcurrentHashMap | H2 In-Memory + Spring Data JPA | 시드 데이터 자동 적재 |
| 의존성 관리 | dependency-management 플러그인 | Gradle native platform BOM | |
| Gradle | 9.0 | 9.0 (유지) | Boot 4 요구사항 충족 |

## 4. 모듈 구조 설계

기능 단위로 모듈을 분리하고, 각 모듈 내부는
`controller / service / repository / domain / dto` 로 구성한다.
모듈 간 참조를 최소화해 추후 Gradle 멀티모듈·마이크로서비스 분리가 쉽도록 한다.

```
com.pebble.mvp
├── common        # 예외/에러 응답 등 공통 인프라
├── config        # 시드 데이터 초기화
├── user          # 회원가입/로그인(더미 토큰)/계정
├── matching      # 추천, 매칭 요청/응답, 매칭 엔진(확장 접점)
├── qna           # 1:1 문의 (유저 등록, 관리자 답변)
├── notification  # 공지/알림
└── admin         # 관리자 대시보드·회원관리·통계
```

## 5. 관리자 모드 기능 명세

| 기능 | 엔드포인트 | 설명 |
| :--- | :--- | :--- |
| 회원 목록 | GET /api/admin/users | 전체 회원 조회 |
| 회원 상태 변경 | PATCH /api/admin/users/{id}/status | PENDING 승인, ACTIVE ↔ SUSPENDED 제재 |
| QnA 목록 | GET /api/admin/qna?status= | 전체/답변대기 문의 조회 |
| QnA 답변 | POST /api/admin/qna/{id}/answer | 관리자 답변 등록 |
| 알림 등록 | POST /api/admin/notifications | 전체 공지 또는 특정 회원 대상 알림 |
| 알림 목록 | GET /api/admin/notifications | 등록된 알림 조회 |
| 매칭 통계 | GET /api/admin/stats/matches | 총 매칭/성사율, 일별·성별·상태별 분포 |

유저 측: 회원가입/로그인, 추천 목록, 매칭 요청/수락/거절, 내 매칭,
QnA 등록/조회, 내 알림 조회.

## 6. AI/외부 서버 연동 확장 설계

```
MatchingService ──▶ MatchingEngine (interface)
                    ├── LocalMatchingEngine       # matching.engine=local (기본, 규칙 기반)
                    └── ExternalAiMatchingEngine  # matching.engine=external-ai (RestClient)
```

`application.yml`의 `matching.engine`을 `external-ai`로 바꾸고
`matching.ai.base-url`만 채우면 코드 수정 없이 외부 AI 매칭 서버로 전환된다.

## 7. 검증 계획

1. `./gradlew build` — 컴파일 + 단위/통합 테스트 통과
2. `./gradlew bootRun` — 로컬 기동 후 전체 API를 curl로 호출 검증
   (로그인 → 추천 → 매칭 요청/수락, QnA 등록 → 관리자 답변, 알림 등록 → 수신 확인, 통계 조회)
3. 정적 콘솔(index.html, admin.html) 접근 확인
4. 완료 문서(phase2_report.md)에 결과 기록
