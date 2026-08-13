-- V4: 관리자 통계/목록 조회용 인덱스
-- AdminStatsService의 집계가 애플리케이션 스트림에서 DB GROUP BY로 이동하면서
-- 그룹핑 키와 조인 키에 인덱스를 부여해 풀스캔 대신 인덱스 스캔이 서도록 한다.
-- (V3은 사용자 모드의 채팅 테이블 마이그레이션이었고, 관리자 전용 전환과 함께 제거되었다.
--  버전 번호는 재사용하지 않고 건너뛴다.)
-- PostgreSQL/H2 공통 문법으로 작성한다.

-- 일별 매칭 집계: group by cast(created_at as date), 만료 배치의 createdAt < cutoff 조회
create index idx_match_records_created_at on match_records (created_at);

-- 상태별 매칭 집계 + 만료 배치의 status 필터
create index idx_match_records_status on match_records (status);

-- 성별 매칭 집계: match_records ⋈ users (requester_id = users.id)
create index idx_match_records_requester_id on match_records (requester_id);

-- 관리자 문의 목록: status 필터 + createdAt 정렬
create index idx_qna_status_created_at on qna (status, created_at);

-- 관리자 알림 발신 이력: createdAt 정렬
create index idx_notifications_created_at on notifications (created_at);
