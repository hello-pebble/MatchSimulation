-- V2: 매칭 자동 만료 상태 추가 (7일 경과 REQUESTED → EXPIRED 배치)
-- 허용 값 목록은 check 제약이 소유하므로 제약을 교체한다.
alter table match_records
    drop constraint ck_match_records_status;

alter table match_records
    add constraint ck_match_records_status
        check (status in ('ACCEPTED', 'REJECTED', 'REQUESTED', 'EXPIRED'));
