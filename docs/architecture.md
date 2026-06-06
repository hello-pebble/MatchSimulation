# MatchSimulation Architecture & Flow Document

본 문서는 MatchSimulation 프로젝트의 기술 아키텍처와 서비스 흐름을 시각화하여 기술합니다.

## 1. 시스템 아키텍처 (System Architecture)

프로젝트는 표준 Layered Architecture를 따르며, Phase 1에서는 인메모리 저장소를 사용합니다.

```mermaid
graph TD
    subgraph "Client Layer"
        Web[Web Browser / Mobile App]
    end

    subgraph "API Layer (Controller)"
        PC[ProfileController]
        AC[AIController]
        MC[MatchController]
        ADC[AdminController]
    end

    subgraph "Service Layer"
        MS[MemberService]
        AS[AIService]
        MTS[MatchingService]
    end

    subgraph "Persistence Layer (In-Memory)"
        UR[UserRepository]
        UPR[UserProfileRepository]
        MR[MatchRepository]
        CRR[ChatRoomRepository]
    end

    Web --> PC
    Web --> AC
    Web --> MC
    Web --> ADC

    PC --> MS
    AC --> AS
    MC --> MTS
    MC --> MS
    ADC --> UR

    MS --> UR
    MS --> UPR
    AS --> UPR
    MTS --> MR
    MTS --> CRR
```

---

## 2. 서비스 핵심 플로우 (Service Flow)

사용자가 가입 후 실제 매칭에 이르기까지의 주요 여정입니다.

```mermaid
flowchart LR
    Start((시작)) --> Onboarding[프로필 작성]
    Onboarding --> Interview[AI 가치관 인터뷰]
    Interview --> Search[회원 탐색 및 필터링]
    Search --> Request[상대방 선택/매칭 요청]
    Request --> Check{교차 매칭?}
    Check -- No --> Wait[매칭 대기열 저장]
    Check -- Yes --> Success[1:1 대화방 생성]
    Success --> End((매칭 완료))
```

---

## 3. 교차 매칭 시퀀스 (Matching Logic Sequence)

상호 선택 시 대화방이 생성되는 백엔드 로직의 상세 흐름입니다.

```mermaid
sequenceDiagram
    participant A as User A (Sender)
    participant S as MatchingService
    participant R as MatchRepository
    participant C as ChatRoomRepository
    participant B as User B (Receiver)

    A->>S: requestMatch(A, B)
    S->>R: save(Match A->B)
    S->>R: findBySenderIdAndReceiverId(B, A)
    
    alt 역방향 매칭 존재함 (B가 이미 A를 선택)
        R-->>S: Match Object Found
        S->>C: save(ChatRoom A-B)
        S-->>A: Match Success (True)
        Note over A,B: 1:1 대화방 활성화
    else 역방향 매칭 없음
        R-->>S: Empty
        S-->>A: Match Pending (False)
    end
```

---

## 4. 도메인 데이터 구조 (Logical ERD)

```mermaid
erDiagram
    USER ||--|| USER_PROFILE : "has"
    USER ||--o{ MATCH : "sends"
    USER ||--o{ CHAT_ROOM : "participates"
    
    USER {
        Long id PK
        String email
        String password
        Enum role
        Enum status
    }
    
    USER_PROFILE {
        Long userId FK
        String name
        Integer age
        String job
        String location
        String aiResult
    }
    
    MATCH {
        Long id PK
        Long senderId FK
        Long receiverId FK
        DateTime createdAt
    }
    
    CHAT_ROOM {
        Long id PK
        Long user1Id FK
        Long user2Id FK
        DateTime createdAt
    }
```

---

## 5. 향후 확장 계획

| 단계 | 내용 | 시각화 목표 |
| :--- | :--- | :--- |
| **Phase 2** | DB 연동 | 외부 RDBMS(MySQL) 아이콘 및 연결선 추가 |
| **Phase 3** | AI 고도화 | Gemini API External Cloud 연동 구조 |
| **Phase 4** | 실시간성 | WebSocket / Redis Pub-Sub 레이어 추가 |
