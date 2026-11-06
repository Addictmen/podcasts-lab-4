package com.example.podcasts.config;

import com.example.podcasts.model.AccountStatus;
import com.example.podcasts.model.Role;
import com.example.podcasts.model.User;
import com.example.podcasts.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Value("${app.admin.email:}") private String email;
    @Value("${app.admin.password:}") private String password;
    @Value("${app.admin.name:Administrator}") private String name;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (User user : users.findAll()) {
            boolean changed = false;
            if (user.getRole() == null) { user.setRole(Role.USER); changed = true; }
            if (user.getAccountStatus() == null) { user.setAccountStatus(AccountStatus.BLOCKED); changed = true; }
            if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
                user.setPasswordHash(encoder.encode(UUID.randomUUID().toString()));
                user.setAccountStatus(AccountStatus.BLOCKED);
                changed = true;
            }
            if (changed) users.save(user);
        }
        if (email.isBlank() && password.isBlank()) return;
        if (email.isBlank() || !isStrong(password)) {
            throw new IllegalStateException("APP_ADMIN_EMAIL и надёжный APP_ADMIN_PASSWORD должны быть заданы вместе");
        }
        String normalized = email.trim().toLowerCase();
        User admin = users.findByEmailIgnoreCase(normalized).orElseGet(User::new);
        admin.setName(admin.getId() == null ? name : admin.getName());
        admin.setEmail(normalized);
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVE);
        users.save(admin);
    }

    private boolean isStrong(String value) {
        return value != null && value.length() >= 12 && value.length() <= 72
                && value.matches(".*[a-z].*") && value.matches(".*[A-Z].*")
                && value.matches(".*\\d.*") && value.matches(".*[^A-Za-z0-9].*");
    }
}
