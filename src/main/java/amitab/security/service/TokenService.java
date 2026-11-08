package amitab.security.service;

import amitab.security.dto.TokenPair;
import amitab.security.exception.TokenReuseException;
import amitab.security.models.Token;
import amitab.security.models.User;
import amitab.security.repository.TokenRepository;
import amitab.security.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.refreshTTL}")
    private Long refreshTokenTTL;

    public TokenPair issueToken(User user) {
        String accessToken = jwtService.issueToken(user);
        String refreshToken = refreshTokenService.issueToken();
        String hashedRefreshToken = refreshTokenService.hashToken(refreshToken);

        Token t = new Token();
        t.setRefreshToken(hashedRefreshToken);
        t.setUser(user);
        t.setExpiredAt(Instant.now().plusSeconds(refreshTokenTTL));

        tokenRepository.save(t);
        return new TokenPair(accessToken, refreshToken);
    }

    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        String hashedToken = refreshTokenService.hashToken(refreshToken);
        tokenRepository.findByRefreshToken(hashedToken)
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    @Transactional(dontRollbackOn = TokenReuseException.class)
    public TokenPair rotateRefreshToken(String refreshToken) {
        String hashedToken = refreshTokenService.hashToken(refreshToken);
        Instant now = Instant.now();
        Token token = tokenRepository.findByRefreshToken(hashedToken)
                .orElseThrow(() -> new RuntimeException("Token not found"));
        User user = token.getUser();

        // Make sure to revoke all users as token reuse detected
        if (token.getRevokedAt() != null) {
            tokenRepository.revokeAllForUser(user.getId(), now);
            throw new TokenReuseException("Refresh token reuse detected");
        }

        if (token.getExpiredAt().isBefore(now)) {
            throw new RuntimeException("Refresh token is expired");
        }
        token.setRevokedAt(now);
        return issueToken(user);
    }
}
