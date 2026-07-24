package com.pebble.mvp.config;

import com.pebble.mvp.domain.MatchRecord;
import com.pebble.mvp.domain.Notification;
import com.pebble.mvp.domain.Qna;
import com.pebble.mvp.domain.User;
import com.pebble.mvp.domain.enums.*;
import com.pebble.mvp.repository.MatchRecordRepository;
import com.pebble.mvp.repository.NotificationRepository;
import com.pebble.mvp.repository.QnaRepository;
import com.pebble.mvp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * H2 In-Memory DB 시드 데이터 적재.
 * 관리자 1명 + 일반회원 20명 + 최근 7일 매칭 기록 + 문의/알림 샘플.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MatchRecordRepository matchRecordRepository;
    private final QnaRepository qnaRepository;
    private final NotificationRepository notificationRepository;

    private static final String[] MALE_NAMES = {"김민준", "이서준", "박도윤", "최시우", "정하준", "강지호", "조은우", "윤선우", "임유준", "한준서"};
    private static final String[] FEMALE_NAMES = {"김서연", "이하은", "박지우", "최서현", "정하윤", "강민서", "조지아", "윤수아", "임예은", "한다은"};
    private static final String[] JOBS = {"개발자", "디자이너", "마케터", "교사", "간호사", "회계사"};
    private static final String[] LOCATIONS = {"서울", "경기", "인천", "부산"};

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        Random random = new Random(42);
        LocalDateTime now = LocalDateTime.now();

        userRepository.save(User.builder()
                .email("admin@match.com").password("admin1234").name("관리자")
                .role(Role.ADMIN).status(UserStatus.ACTIVE).createdAt(now.minusDays(30))
                .build());

        List<User> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            users.add(seedUser(random, now, i, Gender.MALE, MALE_NAMES[i], "male" + (i + 1)));
            users.add(seedUser(random, now, i, Gender.FEMALE, FEMALE_NAMES[i], "female" + (i + 1)));
        }
        userRepository.saveAll(users);

        List<User> active = users.stream().filter(u -> u.getStatus() == UserStatus.ACTIVE).toList();
        MatchStatus[] statuses = MatchStatus.values();
        List<MatchRecord> matches = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            User requester = active.get(random.nextInt(active.size()));
            User partner = active.stream()
                    .filter(u -> u.getGender() != requester.getGender())
                    .skip(random.nextInt(8))
                    .findFirst()
                    .orElse(active.get(0));
            matches.add(MatchRecord.builder()
                    .requesterId(requester.getId())
                    .partnerId(partner.getId())
                    .status(statuses[random.nextInt(statuses.length)])
                    .score(50 + random.nextInt(50) + random.nextInt(10) / 10.0)
                    .createdAt(now.minusDays(random.nextInt(7)).minusHours(random.nextInt(24)))
                    .build());
        }
        matchRecordRepository.saveAll(matches);

        User asker1 = users.get(0);
        User asker2 = users.get(1);
        qnaRepository.save(Qna.builder()
                .userId(asker1.getId()).title("프로필 사진은 어떻게 등록하나요?")
                .question("가입은 했는데 프로필 사진 등록 메뉴를 찾지 못했습니다.")
                .status(QnaStatus.WAITING).createdAt(now.minusDays(2))
                .build());
        qnaRepository.save(Qna.builder()
                .userId(asker2.getId()).title("매칭 추천 기준이 궁금합니다")
                .question("추천 목록은 어떤 기준으로 정렬되나요?")
                .answer("지역/나이/직군 기반 점수로 정렬되며, 추후 AI 모델이 적용될 예정입니다.")
                .status(QnaStatus.ANSWERED).createdAt(now.minusDays(5)).answeredAt(now.minusDays(4))
                .build());
        qnaRepository.save(Qna.builder()
                .userId(asker1.getId()).title("계정 상태가 PENDING입니다")
                .question("언제 승인되나요?")
                .status(QnaStatus.WAITING).createdAt(now.minusHours(6))
                .build());

        notificationRepository.save(Notification.builder()
                .title("서비스 오픈 안내").message("MatchSimulation 베타 서비스가 오픈되었습니다.")
                .createdAt(now.minusDays(7)).build());
        notificationRepository.save(Notification.builder()
                .targetUserId(asker1.getId()).title("프로필 승인 안내")
                .message("회원님의 프로필 심사가 진행 중입니다.")
                .createdAt(now.minusDays(1)).build());

        log.info("시드 데이터 적재 완료: users={}, matches={}, qna={}, notifications={}",
                userRepository.count(), matchRecordRepository.count(),
                qnaRepository.count(), notificationRepository.count());
    }

    private User seedUser(Random random, LocalDateTime now, int index, Gender gender, String name, String emailPrefix) {
        UserStatus status = index < 8 ? UserStatus.ACTIVE : (index == 8 ? UserStatus.PENDING : UserStatus.SUSPENDED);
        return User.builder()
                .email(emailPrefix + "@match.com")
                .password("pass1234")
                .name(name)
                .age(24 + random.nextInt(12))
                .gender(gender)
                .job(JOBS[random.nextInt(JOBS.length)])
                .location(LOCATIONS[random.nextInt(LOCATIONS.length)])
                .role(Role.USER)
                .status(status)
                .createdAt(now.minusDays(random.nextInt(30)))
                .build();
    }
}
