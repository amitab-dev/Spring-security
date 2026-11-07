package amitab.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 6, message = "Password must be 6 character long")
        String password
) {
}
