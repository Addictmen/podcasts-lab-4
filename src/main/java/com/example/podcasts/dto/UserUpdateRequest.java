package com.example.podcasts.dto;

import com.example.podcasts.model.AccountStatus;
import com.example.podcasts.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(@Size(min = 2, max = 100) String name,
                                @Email @Size(max = 254) String email,
                                String password,
                                Role role, AccountStatus accountStatus) { }
