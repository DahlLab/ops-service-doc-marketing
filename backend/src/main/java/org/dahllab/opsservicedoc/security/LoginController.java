package org.dahllab.opsservicedoc.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentifizierung", description = "Auskunft über den aktuell eingeloggten Nutzer (GitHub-OAuth2-Login)")
@RestController

@RequestMapping("/api/auth")
public class LoginController {
    @Operation(
            summary = "Eingeloggten Nutzer abrufen",
            description = "Liefert den GitHub-Usernamen des aktuell per OAuth2 eingeloggten Nutzers."
    )
    @ApiResponse(responseCode = "200", description = "GitHub-Username des eingeloggten Nutzers")
    @ApiResponse(responseCode = "401", description = "Kein Nutzer eingeloggt", content = @Content)
    @GetMapping("/me")

    public String getMe(@AuthenticationPrincipal OAuth2User user) {
        return user
                .getAttributes()
                .get("login")
                .toString();
    }
}
