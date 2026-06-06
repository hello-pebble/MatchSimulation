package com.pebble.mvp;

import com.pebble.mvp.domain.User;
import com.pebble.mvp.domain.UserProfile;
import com.pebble.mvp.domain.enums.Role;
import com.pebble.mvp.domain.enums.UserStatus;
import com.pebble.mvp.repository.UserProfileRepository;
import com.pebble.mvp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    public void run(String... args) {
        // Create Admin
        User admin = User.builder()
                .id(1L)
                .email("admin@test.com")
                .password("admin123")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(admin);

        // Create Users
        createUser(2L, "user1@test.com", "User One", 25, "Male", "Developer", "Seoul");
        createUser(3L, "user2@test.com", "User Two", 28, "Female", "Designer", "Busan");
        createUser(4L, "user3@test.com", "User Three", 30, "Male", "Doctor", "Incheon");

        System.out.println("Initial data populated successfully.");
    }

    private void createUser(Long id, String email, String name, int age, String gender, String job, String location) {
        User user = User.builder()
                .id(id)
                .email(email)
                .password("password123")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        UserProfile profile = UserProfile.builder()
                .userId(id)
                .name(name)
                .age(age)
                .gender(gender)
                .job(job)
                .location(location)
                .aiAnalysisResult("Standard persona for " + name)
                .build();
        userProfileRepository.save(profile);
    }
}
