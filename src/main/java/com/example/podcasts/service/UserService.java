
package com.example.podcasts.service;

import com.example.podcasts.dto.RegistrationRequest;
import com.example.podcasts.dto.UserUpdateRequest;
import com.example.podcasts.model.*;
import com.example.podcasts.repository.PodcastRepository;
import com.example.podcasts.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PodcastRepository podcastRepository;

    @Autowired
    private PodcastService podcastService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @PreAuthorize("@authz.isSelf(authentication, #id)")
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Пользователь не найден"
                ));
    }

    @Transactional
    @PreAuthorize("permitAll()")
    public User register(RegistrationRequest data) {
        String email = data.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email уже зарегистрирован");
        }
        User user = new User();
        user.setName(data.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(data.password()));
        user.setRole(Role.USER);
        user.setAccountStatus(AccountStatus.ACTIVE);
        return userRepository.save(user);
    }

    @Transactional
    @PreAuthorize("@authz.isSelf(authentication, #id)")
    public User update(Long id, UserUpdateRequest data, Authentication authentication) {
        User user = getById(id);

        if (data.name() != null) {
            if (data.name().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Имя не может быть пустым"
                );
            }
            user.setName(data.name().trim());
        }

        if (data.email() != null) {
            if (data.email().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Email не может быть пустым"
                );
            }
            String email = data.email().trim().toLowerCase();
            userRepository.findByEmailIgnoreCase(email)
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> { throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Email уже зарегистрирован"); });
            user.setEmail(email);
        }

        if (data.password() != null) {
            validatePassword(data.password());
            user.setPasswordHash(passwordEncoder.encode(data.password()));
        }

        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if ((data.role() != null || data.accountStatus() != null) && !admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Изменять роль и состояние может только администратор");
        }
        if (admin && data.role() != null) user.setRole(data.role());
        if (admin && data.accountStatus() != null) user.setAccountStatus(data.accountStatus());

        return userRepository.save(user);
    }

    @Transactional
    @PreAuthorize("@authz.isSelf(authentication, #id)")
    public void delete(Long id) {
        getById(id);

        // Сначала удаляем подкасты пользователя
        for (Podcast podcast : podcastRepository.findByAuthorId(id)) {
            podcastService.delete(podcast.getId());
        }

        // Затем удаляем самого пользователя
        userRepository.deleteById(id);
    }

    private void validatePassword(String password) {
        if (password.length() < 12 || password.length() > 72 ||
                !password.matches(".*[a-z].*") || !password.matches(".*[A-Z].*") ||
                !password.matches(".*\\d.*") || !password.matches(".*[^A-Za-z0-9].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Пароль должен быть длиной 12-72 символа и содержать строчную и заглавную буквы, цифру и специальный символ");
        }
    }
}
