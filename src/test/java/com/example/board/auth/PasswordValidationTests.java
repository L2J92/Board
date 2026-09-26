package com.example.board.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordValidationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        String secret = Base64.getEncoder().encodeToString(key);
        registry.add("jwt.secret", () -> secret);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:password_validation;DB_CLOSE_DELAY=-1");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Test
    void minimumLengthAsciiPasswordCanSignUpAndLogIn() throws Exception {
        signUpAndLogIn("Ab12!@#$");
    }

    @Test
    void maximumLengthAsciiPasswordCanSignUpAndLogIn() throws Exception {
        signUpAndLogIn("a".repeat(64));
    }

    @Test
    void invalidCharactersAndLengthsReturn400ForSignUpAndLogin() throws Exception {
        for (String password : new String[]{
                "가".repeat(8), "Abcd123😀", "Abcd 123!", "Abcd\t123!",
                "Abcd123\u007f", "Ab12!@#", "a".repeat(65)
        }) {
            String email = UUID.randomUUID() + "@example.com";
            mvc.perform(post("/member").contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "id", email, "password", password, "nickName", "tester"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
            mvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "email", email, "password", password))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void blankAndMissingPasswordsStillReturnValidationError() throws Exception {
        for (String passwordField : new String[]{",\"password\":\"\"", ""}) {
            mvc.perform(post("/member").contentType(APPLICATION_JSON)
                            .content("{\"id\":\"blank@example.com\",\"nickName\":\"tester\"" + passwordField + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
            mvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                            .content("{\"email\":\"blank@example.com\"" + passwordField + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    private void signUpAndLogIn(String password) throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        mvc.perform(post("/member").contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", email, "password", password, "nickName", "tester"))))
                .andExpect(status().isOk());
        mvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
}
