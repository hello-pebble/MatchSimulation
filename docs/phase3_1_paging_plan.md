# Phase 3-1 시작 전 계획 문서 — 페이징/정렬 (Pagination & Sorting)

작성일: 2026-07-30
선행 단계: Phase 2 (기술스택 업그레이드 + 관리자 모드) 완료

## 1. 배경과 목표

목록 API(회원, QnA, 매칭 이력)가 전체 데이터를 한 번에 반환하고 있어
데이터가 늘어나면 응답 크기·조회 시간이 함께 증가한다.
전통적 백엔드의 필수 요소인 **페이지네이션과 정렬**을 Spring Data의
`Pageable`로 도입해 목록 API를 페이지 단위로 전환한다.

## 2. 적용 대상 API

| API | 기본 정렬 | 비고 |
| :--- | :--- | :--- |
| GET /api/admin/users | createdAt DESC | 관리자 회원 목록 |
| GET /api/admin/qna | createdAt DESC | 관리자 문의 목록 (status 필터 유지) |
| GET /api/matching/my | createdAt DESC | 내 매칭 이력 |

요청 파라미터: `?page=0&size=10&sort=createdAt,desc` (0-base page)

## 3. 설계

- **공통 응답 포맷** `common/PageResponse<T>` record 신설:
  `{content, page, size, totalElements, totalPages, hasNext}`
  — Spring `Page` 직접 직렬화의 불안정 포맷 경고를 피하고 API 계약을 고정한다.
- Repository: `Page<T>` 반환 메서드로 확장
  - `MatchRecordRepository.findByRequesterIdOrPartnerId(..., Pageable)`
  - `QnaRepository.findByStatus(status, Pageable)` / `findAll(Pageable)`
  - `UserRepository.findAll(Pageable)` (JpaRepository 기본 제공)
- Controller: `@PageableDefault(size=10, sort="createdAt", direction=DESC)` 적용
- **size 상한 100 클램프** — 과도한 size 요청으로부터 보호
- 허용되지 않은 sort 필드 → `PropertyReferenceException`을
  `GlobalExceptionHandler`에서 400 JSON으로 매핑
- 콘솔: admin.html(회원/문의), index.html(내 매칭)에 page/size 입력과 이전/다음 버튼

## 4. 엣지케이스 정의 (구현 시 검증)

| # | 케이스 | 기대 동작 |
| :--- | :--- | :--- |
| E1 | 범위 밖 page (예: page=999) | 200 + 빈 content, totalPages 정보 유지 |
| E2 | size > 100 | 100으로 클램프 |
| E3 | 존재하지 않는 sort 필드 | 400 + `{status, message}` JSON |
| E4 | 음수 page/size | Spring 기본 보정(0/기본값) 동작 확인 |
| E5 | 마지막 페이지 | hasNext=false |
| E6 | status 필터 + 페이징 조합 (qna) | 필터된 결과 기준으로 페이지 계산 |

## 5. 테스트 계획 (QA)

- MockMvc 통합 테스트 `PagingIntegrationTest`:
  페이지 크기 준수, 정렬 방향, 마지막 페이지 hasNext, 범위 밖 page 빈 목록,
  잘못된 sort 400, size 클램프
- 수동 QA: 콘솔에서 페이지 이동 버튼 동작, curl 실측 → 완료 문서에 기록

## 6. 산출물

- 코드: PageResponse, Repository/Service/Controller 페이징 적용, 콘솔 UI
- 문서: 본 계획 문서, `phase3_1_paging_report.md`(완료·엣지케이스 결과·QA 체크리스트),
  `user_mode.md`/`admin_mode.md` API 명세 갱신
