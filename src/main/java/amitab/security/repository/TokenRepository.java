package amitab.security.repository;

import amitab.security.models.Token;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TokenRepository extends JpaRepository<Token, UUID> {
    Optional<Token> findByRefreshToken(String refreshToken);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                UPDATE Token t
                    SET t.revokedAt = :now, t.updatedAt = :now
                    WHERE t.user.id = :userId AND t.revokedAt IS NULL
            """)
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
