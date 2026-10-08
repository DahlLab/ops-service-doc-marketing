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

@Tag(name = "Authentication", description = "Information about the currently logged-in user (GitHub OAuth2 login)")
@RestController

@RequestMapping("/api/auth")
public class LoginController {
    @Operation(
            summary = "Get the logged-in user",
            description = "Returns the GitHub username of the user currently logged in via OAuth2."
    )
    @ApiResponse(responseCode = "200", description = "GitHub username of the logged-in user")
    @ApiResponse(responseCode = "401", description = "No user logged in", content = @Content)
    @GetMapping("/me")

    public String getMe(@AuthenticationPrincipal OAuth2User user) {
        return user
                .getAttributes()
                .get("login")
                .toString();
    }
}
