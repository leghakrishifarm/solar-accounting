package com.legakrishi.solar.bootstrap;

import com.legakrishi.solar.model.User;
import com.legakrishi.solar.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("prod") // runs only in prod
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepo;

    @Value("${BOOTSTRAP_ADMIN_EMAIL:admin@leghakrishi.com}")
    private String email;

    @Value("${BOOTSTRAP_ADMIN_PASSWORD:ChangeThis#2025}")
    private String rawPassword;

    public AdminBootstrap(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public void run(String... args) {
        userRepo.findByEmail(email).ifPresentOrElse(
                u -> {}, // already exists
                () -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setPassword(new BCryptPasswordEncoder().encode(rawPassword));
                    u.setName("Administrator");
                    u.setRole("ADMIN"); // adjust if your enum/string differs
                    userRepo.save(u);
                }
        );
    }
}
