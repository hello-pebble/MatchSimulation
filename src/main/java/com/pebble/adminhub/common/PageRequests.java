package com.pebble.adminhub.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** 페이징 요청 공통 정책 (size 상한 클램프) */
public final class PageRequests {

    public static final int MAX_SIZE = 100;

    private PageRequests() {
    }

    /** size가 상한을 넘으면 MAX_SIZE로 잘라낸 Pageable을 반환한다. */
    public static Pageable clamp(Pageable pageable) {
        if (pageable.getPageSize() <= MAX_SIZE) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), MAX_SIZE, pageable.getSort());
    }
}
