package amitab.security.dto;

import amitab.security.models.Role;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

public record RegisterResponseDto(
        UUID id,
        String name,
        String email,
        Role role,
        Instant createdAt
) {
}
