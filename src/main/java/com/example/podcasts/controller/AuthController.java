package com.example.podcasts.controller;

import com.example.podcasts.dto.RegistrationRequest;
import com.example.podcasts.model.User;
import com.example.podcasts.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final UserService userService;
    public AuthController(UserService userService) { this.userService = userService; }

    @GetMapping("/csrf")
    @PreAuthorize("permitAll()")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "parameterName", token.getParameterName(),
                "token", token.getToken());
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("permitAll()")
    public Map<String, Object> register(@Valid @RequestBody RegistrationRequest request) {
        return toJson(userService.register(request));
    }

    public static Map<String, Object> toJson(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId()); result.put("name", user.getName());
        result.put("email", user.getEmail()); result.put("role", user.getRole());
        result.put("accountStatus", user.getAccountStatus());
        return result;
    }
}
