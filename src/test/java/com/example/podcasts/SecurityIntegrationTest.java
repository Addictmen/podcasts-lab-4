package com.example.podcasts;

import com.example.podcasts.model.*;
import com.example.podcasts.repository.PodcastRepository;
import com.example.podcasts.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PodcastRepository podcasts;
    @Autowired PasswordEncoder encoder;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        podcasts.deleteAll();
        users.deleteAll();
        alice = saveUser("Alice", "alice@example.com", "StrongPass1!", Role.USER, AccountStatus.ACTIVE);
        bob = saveUser("Bob", "bob@example.com", "OtherStrong1!", Role.USER, AccountStatus.ACTIVE);
    }

    @Test
    void csrfTokenIsIssuedAndMutationWithoutItIsRejected() throws Exception {
        mvc.perform(get("/api/csrf"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New User\",\"email\":\"new@example.com\",\"password\":\"SecurePass1!\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrationUsesBcryptAndAlwaysCreatesOrdinaryActiveUser() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New User\",\"email\":\"NEW@example.com\",\"password\":\"SecurePass1!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User saved = users.findByEmailIgnoreCase("new@example.com").orElseThrow();
        assertThat(saved.getPasswordHash()).startsWith("$2");
        assertThat(encoder.matches("SecurePass1!", saved.getPasswordHash())).isTrue();
    }

    @Test
    void weakAndDuplicateRegistrationAreRejected() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Weak\",\"email\":\"weak@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice 2\",\"email\":\"ALICE@example.com\",\"password\":\"SecurePass1!\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void authenticationAndOwnershipReturn401And403() throws Exception {
        Podcast podcast = new Podcast();
        podcast.setTitle("Alice podcast"); podcast.setAuthorId(alice.getId()); podcast.setStatus(PodcastStatus.DRAFT);
        podcast = podcasts.save(podcast);

        mvc.perform(get("/api/podcasts/{id}", podcast.getId()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/podcasts/{id}", podcast.getId()).with(httpBasic("bob@example.com", "OtherStrong1!")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/podcasts/{id}", podcast.getId()).with(httpBasic("alice@example.com", "StrongPass1!")))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotPromoteOrUnblockSelfAndBlockedAccountCannotAuthenticate() throws Exception {
        mvc.perform(patch("/api/users/{id}", alice.getId()).with(httpBasic("alice@example.com", "StrongPass1!"))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        assertThat(users.findById(alice.getId()).orElseThrow().getRole()).isEqualTo(Role.USER);

        bob.setAccountStatus(AccountStatus.BLOCKED); users.save(bob);
        mvc.perform(get("/api/users/{id}", bob.getId()).with(httpBasic("bob@example.com", "OtherStrong1!")))
                .andExpect(status().isUnauthorized());
    }

    private User saveUser(String name, String email, String password, Role role, AccountStatus status) {
        User user = new User(); user.setName(name); user.setEmail(email);
        user.setPasswordHash(encoder.encode(password)); user.setRole(role); user.setAccountStatus(status);
        return users.save(user);
    }
}
