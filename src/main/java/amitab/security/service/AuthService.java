package amitab.security.service;

import amitab.security.dto.RegisterRequestDto;
import amitab.security.dto.RegisterResponseDto;
import amitab.security.models.User;
import amitab.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterResponseDto register(RegisterRequestDto body) {
        String email = User.normalizeEmail(body.email());
        if (userRepository.existByEmail(email)) {
            throw new RuntimeException("Email is taken");
        }
        User u = new User();
        u.setName(body.name());
        u.setEmail(User.normalizeEmail(body.email()));
        u.setPassword(passwordEncoder.encode(body.password()));

        User savedUser = userRepository.save(u);
        return new RegisterResponseDto(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getCreatedAt()
        );
    }
}
