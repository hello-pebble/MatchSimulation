-- V2: 매칭 자동 만료 상태 추가 (7일 경과 REQUESTED → EXPIRED 배치)
alter table match_records
    alter column status enum ('ACCEPTED', 'REJECTED', 'REQUESTED', 'EXPIRED') not null;
