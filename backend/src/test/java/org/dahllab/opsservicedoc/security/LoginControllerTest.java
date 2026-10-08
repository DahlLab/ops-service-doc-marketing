package org.dahllab.opsservicedoc.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest

@AutoConfigureMockMvc
class LoginControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GIVEN a logged-in GitHub user WHEN /api/auth/me is called THEN the username is returned")
    void getMe_returnsUsername_whenUserIsAuthenticated() throws Exception {
        String expectedUsername = "testuser";
        Map<String, Object> fakeGithubAttribute = Map.of("login", expectedUsername);

        var result = mockMvc.perform(
                get("/api/auth/me")
                        .with(oauth2Login().attributes(attrs -> attrs.putAll(fakeGithubAttribute)))
        );

        result
                .andExpect(status().isOk())
                .andExpect(content().string(expectedUsername));
    }

    @Test
    @DisplayName("GIVEN no logged-in user WHEN /api/auth/me is called THEN 401 Unauthorized is returned")
    void getMe_returns401_whenUserIsNotAuthenticated() throws Exception {
        var result = mockMvc.perform(
                get("/api/auth/me")
        );

        result
                .andExpect(status().isUnauthorized());
    }
}
