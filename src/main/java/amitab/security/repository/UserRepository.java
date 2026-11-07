package amitab.security.repository;

import amitab.security.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existByEmail(String email);

    Optional<User> findByEmail(String email);
}
