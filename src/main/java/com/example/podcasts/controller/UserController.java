package com.example.podcasts.controller;

import com.example.podcasts.dto.RegistrationRequest;
import com.example.podcasts.dto.UserUpdateRequest;
import com.example.podcasts.model.User;
import com.example.podcasts.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    @GetMapping
    public List<Map<String, Object>> getAll() {
        return userService.getAll().stream().map(AuthController::toJson).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOne(@PathVariable Long id) {
        return AuthController.toJson(userService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody RegistrationRequest data) {
        return AuthController.toJson(userService.register(data));
    }

    @PatchMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest data,
                                      Authentication authentication) {
        return AuthController.toJson(userService.update(id, data, authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { userService.delete(id); }
}
