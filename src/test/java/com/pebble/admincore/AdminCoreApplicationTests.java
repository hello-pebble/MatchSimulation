package com.pebble.admincore;

import com.pebble.admincore.admin.dto.AdminDtos.MatchStatsResponse;
import com.pebble.admincore.user.repository.UserRepository;
import com.pebble.admincore.admin.service.AdminStatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AdminCoreApplicationTests {

    @Autowired
    UserRepository userRepository;

    @Autowired
    AdminStatsService adminStatsService;

    @Test
    void 컨텍스트가_로드되고_시드_데이터가_적재된다() {
        assertThat(userRepository.count()).isEqualTo(21); // 관리자 1 + 회원 20
    }

    @Test
    void 매칭_통계가_집계된다() {
        MatchStatsResponse stats = adminStatsService.matchStats();
        assertThat(stats.totalMatches()).isEqualTo(30);
        assertThat(stats.daily()).isNotEmpty();
        assertThat(stats.byGender()).isNotEmpty();
        assertThat(stats.byStatus()).isNotEmpty();
    }
}
