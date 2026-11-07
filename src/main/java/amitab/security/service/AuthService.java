package amitab.security.service;

import amitab.security.dto.*;
import amitab.security.models.User;
import amitab.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public RegisterResponseDto register(RegisterRequestDto body) {
        String email = User.normalizeEmail(body.email());
        if (userRepository.existsByEmail(email)) {
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

    public TokenPair login(LoginRequestDto body) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(body.email(), body.password())
        );
        User user = (User) authentication.getPrincipal();
        return tokenService.issueToken(user);
    }
}
