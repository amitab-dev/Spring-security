package amitab.security.service;

import amitab.security.dto.TokenPair;
import amitab.security.models.Token;
import amitab.security.models.User;
import amitab.security.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.refreshTokenTtl}")
    private final Long refreshTokenTTL;

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
}
