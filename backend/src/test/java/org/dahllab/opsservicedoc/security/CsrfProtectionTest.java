package org.dahllab.opsservicedoc.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Prüft den CSRF-Schutz der SecurityConfig: ohne Token werden schreibende
// Aufrufe abgelehnt, mit passendem Cookie + Header gehen sie durch, und das
// Token-Cookie wird für das React-Frontend lesbar (nicht HttpOnly) gesetzt.
//
// Eigener Anwendungskontext über die Dummy-Property in @SpringBootTest:
// Spring cached den Kontext zwischen Testklassen anhand seiner
// Konfiguration, und die csrf()-Hilfsmethode aus spring-security-test tauscht
// im CsrfFilter DAUERHAFT das Token-Repository gegen ein Session-Repository
// aus. Läge dieser Test im selben (gecachten) Kontext wie die
// Controller-Tests, würde er nicht mehr mein echtes Cookie-Repository
// prüfen. Durch die zusätzliche Property bekommt er einen eigenen Kontext.
@SpringBootTest(properties = "test.isolierter-csrf-kontext=true")
@AutoConfigureMockMvc
class CsrfProtectionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GIVEN a logged-in user without CSRF token WHEN POST is sent THEN 403 Forbidden is returned")
    void post_returns403_withoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .with(oauth2Login())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GIVEN a matching cookie and header WHEN POST is sent THEN the request passes the CSRF filter")
    void post_passesCsrfFilter_withMatchingCookieAndHeader() throws Exception {
        // Der leere Body ist absichtlich ungültig: 400 (Validierung) statt
        // 403 beweist, dass der CSRF-Filter den Request durchgelassen hat.
        mockMvc.perform(post("/api/tasks")
                        .with(oauth2Login())
                        .cookie(new Cookie("XSRF-TOKEN", "test-token"))
                        .header("X-XSRF-TOKEN", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GIVEN a cookie and a WRONG header WHEN POST is sent THEN 403 Forbidden is returned")
    void post_returns403_withWrongToken() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .with(oauth2Login())
                        .cookie(new Cookie("XSRF-TOKEN", "test-token"))
                        .header("X-XSRF-TOKEN", "anderes-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GIVEN a GET request WHEN it is answered THEN the backend sets the XSRF-TOKEN cookie (not HttpOnly)")
    void get_setsReadableCsrfCookie() throws Exception {
        // "login"-Attribut setze ich, weil der LoginController es ausliest.
        mockMvc.perform(get("/api/auth/me")
                        .with(oauth2Login().attributes(attrs -> attrs.put("login", "testuser"))))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }
}
