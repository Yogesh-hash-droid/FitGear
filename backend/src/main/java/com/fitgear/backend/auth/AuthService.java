package com.fitgear.backend.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }

        // Public registration can NEVER create an admin account.
        AppUser user = new AppUser(
                request.name().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                UserRole.CUSTOMER
        );
        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return new AuthResponse(
            jwtService.generateToken(user),
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole()
        );
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(ProfileResponse::from)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
    }

    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
        user.setName(request.name().trim());
        return ProfileResponse.from(userRepository.save(user));
    }
}
