package amitab.security.controller;

import amitab.security.dto.LoginRequestDto;
import amitab.security.dto.LoginResponseDto;
import amitab.security.dto.RegisterRequestDto;
import amitab.security.dto.RegisterResponseDto;
import amitab.security.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping()
    public ResponseEntity<RegisterResponseDto> register(
            @RequestBody RegisterRequestDto body
    ) {
        return ResponseEntity.status(201).body(authService.register(body));
    }

    public ResponseEntity<LoginResponseDto> login(
            @RequestBody LoginRequestDto body
    ) {
        return ResponseEntity.ok(authService.login(body));
    }


}
