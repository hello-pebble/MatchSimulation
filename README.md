# MatchSimulation

AI 기반 스마트 매칭 소개팅 서비스 MVP 프로젝트입니다. 유저의 가치관과 대화 스타일을 AI가 분석하여 최적의 상대방을 매칭하는 기능을 제공합니다.

## 1. 프로젝트 개요

| 항목 | 내용 |
| :--- | :--- |
| 프로젝트명 | MatchSimulation |
| 주요 가치 | 실제 매칭 전 AI 시뮬레이션을 통한 매칭률 향상 및 대화 코칭 |
| 개발 단계 | Phase 1 (MVP 핵심 로직 구현 완료) |
| 아키텍처 | Spring Boot Monolith (In-Memory 기반) |

## 2. 주요 기능

| 기능 분류 | 상세 내용 |
| :--- | :--- |
| 사용자 온보딩 | 기본 인적 사항(나이, 직업, 지역 등) 입력 및 프로필 구축 |
| AI 이상형 인터뷰 | 챗봇 형태의 AI 인터뷰를 통해 유저의 가치관 및 연애 스타일 분석 |
| AI 대화 시뮬레이션 | 가상의 이상형과 대화를 진행하고 대화 스타일에 대한 피드백 수신 |
| 회원 탐색 | 성향 매칭률 기반의 회원 리스트 정렬 및 필터링 기능 |
| 교차 매칭 시스템 | 두 유저가 서로를 선택한 경우에만 1:1 대화방이 생성되는 더블 블라인드 매커니즘 |
| 관리자 대시보드 | 전체 가입자 수 통계 모니터링 및 불량 회원 제재 관리 |

## 3. 기술 스택

| 구분 | 기술 |
| :--- | :--- |
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.3.0 |
| 빌드 도구 | Gradle |
| 주요 라이브러리 | Spring Web, Spring Validation, Lombok, JUnit 5 |
| 저장소 | In-Memory (ConcurrentHashMap) |

## 4. API 명세 (핵심 엔드포인트)

| 도메인 | 메서드 | 엔드포인트 | 설명 |
| :--- | :--- | :--- | :--- |
| 프로필 | POST | /api/profiles | 신규 프로필 작성 및 온보딩 |
| AI 서비스 | POST | /api/ai/interview | AI 가치관 인터뷰 답변 제출 및 분석 |
| AI 서비스 | POST | /api/ai/simulate | 가상 대화 시뮬레이션 및 피드백 요청 |
| 매칭 | GET | /api/matches/members/{id} | 매칭률 기반 타 회원 목록 조회 |
| 매칭 | POST | /api/matches/request | 특정 회원에게 매칭 요청 (교차 시 성사) |
| 매칭 | GET | /api/matches/chat-rooms/{id} | 현재 활성화된 1:1 대화방 목록 조회 |
| 어드민 | GET | /api/admin/dashboard | 가입자 통계 및 서비스 현황 조회 |

## 5. 실행 방법

| 단계 | 명령어 |
| :--- | :--- |
| 빌드 및 실행 | ./gradlew bootRun |
| 테스트 실행 | ./gradlew test |

## 6. 향후 개발 계획

| 단계 | 주요 작업 내용 |
| :--- | :--- |
| Phase 2 | 관계형 데이터베이스(RDBMS) 연동 및 JPA 적용 |
| Phase 3 | 실제 LLM(Gemini API 등) 연동을 통한 AI 로직 고도화 |
| Phase 4 | 프론트엔드 UI 통합 및 실시간 채팅 기능 구현 |
