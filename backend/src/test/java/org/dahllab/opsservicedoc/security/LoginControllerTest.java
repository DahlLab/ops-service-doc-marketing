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


// @SpringBootTest: startet den kompletten Spring-Anwendungskontext für den Test
// (nicht nur einen Ausschnitt) - ich brauche das, weil mein SecurityConfig
// mit eingebunden werden muss, damit die Security-Regeln greifen
@SpringBootTest
// @AutoConfigureMockMvc: stellt mir ein MockMvc-Objekt bereit, mit dem ich
// HTTP-Requests SIMULIEREN kann, ohne einen echten Server zu starten
// und ohne einen echten Browser zu brauchen
@AutoConfigureMockMvc
class LoginControllerTest {

    // Spring injiziert hier automatisch das fertig konfigurierte MockMvc-Objekt
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GIVEN ein eingeloggter GitHub-User WHEN /api/auth/me aufgerufen wird THEN wird der Username zurückgegeben")
    void getMe_returnsUsername_whenUserIsAuthenticated() throws Exception {

        // GIVEN:
        // Ich bereite die Testdaten vor. Statt eines echten GitHub-Logins
        // bauen ich eine "Map" mit den Attributen, die GitHub normalerweise
        // nach einem echten Login zurückliefern würde (z.B. "login" = Username).
        // Das entspricht genau dem, was user.getAttributes().get("login")
        // im echten LoginController später ausliest.
        String expectedUsername = "testuser";
        Map<String, Object> fakeGithubAttribute = Map.of("login", expectedUsername);

        // WHEN:
        // Ich simuliere einen GET-Request auf /api/auth/me.
        // ".with(oauth2Login().attributes(...))" ist der entscheidende Trick:
        // Er sagt MockMvc "tu so, als wäre dieser Request bereits erfolgreich
        // über OAuth2 eingeloggt, und die Attribute des Users sind die, die ich hier übergebe".
        // Dadurch muss ich NICHT wirklich gegen GitHub authentifizieren, um den Test zu schreiben.
        var result = mockMvc.perform(
                get("/api/auth/me")
                        .with(oauth2Login().attributes(attrs -> attrs.putAll(fakeGithubAttribute)))
        );

        // THEN:
        // Ich prüfe zwei Dinge:
        // 1. Der HTTP-Status muss 200 OK sein (da der User ja "eingeloggt" ist)
        // 2. Der zurückgegebene Text muss exakt unserem erwarteten Usernamen entsprechen
        result
                .andExpect(status().isOk())
                .andExpect(content().string(expectedUsername));
    }

    @Test
    @DisplayName("GIVEN kein eingeloggter User WHEN /api/auth/me aufgerufen wird THEN kommt 401 Unauthorized")
    void getMe_returns401_whenUserIsNotAuthenticated() throws Exception {

        // GIVEN:
        // Hier brauche ich keine Vorbereitung - der Zustand "nicht eingeloggt"
        // ist einfach der Standardzustand, wenn wir KEIN ".with(oauth2Login())" übergeben.

        // WHEN:
        // Ich simuliere denselben Request wie oben, aber diesmal OHNE
        // simulierten Login - also genau der Zustand, den ein echter,
        // nicht angemeldeter Nutzer hätte.
        var result = mockMvc.perform(
                get("/api/auth/me")
        );

        // THEN:
        // Da /api/auth/me in der SecurityConfig als ".authenticated()" markiert ist,
        // UND ich dort den authenticationEntryPoint auf HttpStatus.UNAUTHORIZED gesetzt habe,
        // erwarte ich hier exakt einen 401-Statuscode (nicht z.B. eine Weiterleitung zu einer Login-Seite).
        result
                .andExpect(status().isUnauthorized());
    }
}