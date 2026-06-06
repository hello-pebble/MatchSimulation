# MatchSimulation Architecture Document

본 문서는 MatchSimulation 프로젝트의 Phase 1 기술 아키텍처 및 설계 원칙을 기술합니다.

## 1. 시스템 아키텍처 개요

본 프로젝트는 초기 MVP 단계의 빠른 검증을 위해 **Layered Architecture** 기반의 **Monolithic Spring Boot** 구조를 채택하였습니다.

| 구성 요소 | 기술 스택 | 비고 |
| :--- | :--- | :--- |
| **API Layer** | Spring Web MVC | RESTful API 엔드포인트 제공 |
| **Service Layer** | Spring Service | 비즈니스 로직 및 도메인 흐름 제어 |
| **Repository Layer** | In-Memory (ConcurrentHashMap) | 데이터 영속성 모사 및 테스트 용이성 확보 |
| **Domain Layer** | Java Entities (Lombok) | 핵심 비즈니스 모델 및 규칙 정의 |

## 2. 데이터 흐름 (Data Flow)

| 단계 | 흐름 설명 | 담당 컴포넌트 |
| :--- | :--- | :--- |
| **Request** | 클라이언트로부터 HTTP 요청 수신 및 DTO 검증 | Controller (API Layer) |
| **Processing** | 비즈니스 로직 실행 및 도메인 객체 간 상호작용 | Service (Service Layer) |
| **Persistence** | 메모리 기반 맵에 데이터 저장 및 조회 | Repository (Persistence Layer) |
| **Response** | 처리 결과를 DTO로 변환하여 JSON 응답 반환 | Controller (API Layer) |

## 3. 핵심 설계 패턴 및 원칙

### 3.1. 교차 매칭 알고리즘 (Double-Blind Match)
상호 동의 하에만 연결되는 소개팅 서비스의 핵심 로직입니다.

| 프로세스 단계 | 상세 내용 |
| :--- | :--- |
| **Step 1: Save** | 유저 A가 유저 B를 선택하면 `Match` 객체 생성 및 저장 |
| **Step 2: Check** | 리포지토리에서 `sender=B, receiver=A`인 데이터 존재 여부 즉시 조회 |
| **Step 3: Resolve** | 상호 데이터 존재 시 `ChatRoom` 생성 및 매칭 확정 |

### 3.2. AI 시뮬레이션 인터페이스
향후 실제 LLM 연동을 고려한 추상화 구조입니다.

| 컴포넌트 | 역할 | 현재 구현 (Phase 1) |
| :--- | :--- | :--- |
| **AIService** | 대화 분석 및 피드백 로직 | 정적 규칙 기반 Mock 결과 반환 |
| **Analysis DTO** | 분석 데이터 규격화 | 페르소나 및 점수 기반 JSON 구조 |

## 4. 도메인 모델 설계 (ERD 논리 구조)

| 모델명 | 주요 필드 | 설명 |
| :--- | :--- | :--- |
| **User** | id, email, password, role, status | 인증 및 권한 관리의 핵심 엔터티 |
| **UserProfile** | userId, name, age, gender, job, location, aiResult | 유저별 상세 정보 및 AI 분석 데이터 저장 |
| **Match** | id, senderId, receiverId, createdAt | 유저 간 단방향 호감 표시 기록 |
| **ChatRoom** | id, user1Id, user2Id, createdAt | 매칭 성공 시 생성되는 1:1 대화 세션 |

## 5. 인프라 및 배포 전략 (Phase 1)

| 항목 | 내용 |
| :--- | :--- |
| **Runtime** | Java 17 / Spring Boot 3.3.0 |
| **Build** | Gradle (Local Wrapper 사용) |
| **Storage** | JVM Heap Memory (No External DB) |
| **Environment** | 개발 및 테스트 통합 환경 |

---

## 6. 향후 아키텍처 확장 계획

| 단계 | 확장 내용 | 목적 |
| :--- | :--- | :--- |
| **Step 1** | MySQL / JPA 연동 | 데이터 영속성 및 트랜잭션 보장 |
| **Step 2** | Spring Security 적용 | JWT 기반 인증 및 인가 고도화 |
| **Step 3** | Gemini API 연동 | 실시간 AI 인터뷰 및 분석 엔진 고도화 |
| **Step 4** | WebSocket / Redis | 실시간 채팅 서비스 확장 및 확장성 확보 |
