package amitab.security.dto;

public record TokenPair(
        String accessToken,
        String refreshToken
) {
}
