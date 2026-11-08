package amitab.security.controller;

import amitab.security.dto.*;
import amitab.security.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final static String ACCESS_TOKEN_NAME = "access_token";
    private final static String REFRESH_TOKEN_NAME = "refresh_token";
    private final AuthService authService;

    @PostMapping("/")
    public ResponseEntity<RegisterResponseDto> register(@RequestBody RegisterRequestDto body) {
        return ResponseEntity.status(201).body(authService.register(body));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto body, HttpServletResponse response) {
        TokenPair tokens = authService.login(body);
        setTokensCookies(response, tokens);
        return ResponseEntity.status(200).body(new LoginResponseDto(tokens.accessToken()));
    }

    @GetMapping("/logout")
    public ResponseEntity<MessageResponseDto> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getRefreshToken(request);
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        clearTokenCookies(response);
        return ResponseEntity.status(200).body(new MessageResponseDto("User logged out"));
    }

    @GetMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = getRefreshToken(request);
        if (refreshToken == null) {
           throw new RuntimeException("Unauthorized");
        }
        TokenPair tokens = authService.refreshAccessToken(refreshToken);
        setTokensCookies(response, tokens);
        return ResponseEntity.status(200).body(new LoginResponseDto(tokens.accessToken()));
    }

    private String getRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie ck : cookies) {
            if (REFRESH_TOKEN_NAME.equals(ck.getName())) {
                return ck.getValue();
            }
        }
        return null;
    }

    private void clearTokenCookies(HttpServletResponse res) {
        ResponseCookie accessCookies = setCookies(ACCESS_TOKEN_NAME, "", 0);
        ResponseCookie refreshCookie = setCookies(REFRESH_TOKEN_NAME, "", 0);
        res.addHeader("Set-Cookie", accessCookies.toString());
        res.addHeader("Set-Cookie", refreshCookie.toString());
    }

    private void setTokensCookies(HttpServletResponse res, TokenPair tokens) {
        ResponseCookie accessCookies = setCookies(ACCESS_TOKEN_NAME, tokens.accessToken(), 15);
        ResponseCookie refreshCookie = setCookies(REFRESH_TOKEN_NAME, tokens.refreshToken(), 43200);
        res.addHeader("Set-Cookie", accessCookies.toString());
        res.addHeader("Set-Cookie", refreshCookie.toString());
    }

    private ResponseCookie setCookies(String name, String value, long durationInMins) {
        return ResponseCookie
                .from(name, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("lax")
                .maxAge(Duration.ofMinutes(durationInMins))
                .build();
    }
}
