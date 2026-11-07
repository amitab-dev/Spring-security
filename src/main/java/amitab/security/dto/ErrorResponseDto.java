package amitab.security.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ErrorResponseDto(
        HttpStatus status,
        String message,
        Instant timestamp,
        String path
) {
}
