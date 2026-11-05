package amitab.security.controller;

import amitab.security.dto.RegisterRequestDto;
import amitab.security.dto.RegisterResponseDto;
import amitab.security.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private ResponseEntity<RegisterResponseDto> register(
            @RequestBody RegisterRequestDto body
    ) {
        return ResponseEntity.status(201).body(authService.register(body));
    }
}
