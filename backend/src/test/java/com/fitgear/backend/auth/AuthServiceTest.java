package com.fitgear.backend.auth;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class AuthServiceTest {

    private AuthService service(UserRepository userRepository, JwtService jwtService) {
        return new AuthService(userRepository, new BCryptPasswordEncoder(), jwtService);
    }

    @Test
    void shouldHashPasswordAndCreateCustomer() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        AuthService service = service(userRepository, Mockito.mock(JwtService.class));
        RegisterRequest request = new RegisterRequest("Arun", "ARUN@example.com", "Password123");

        service.register(request);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser savedUser = captor.getValue();

        assertNotEquals("Password123", savedUser.getPassword());
        assertTrue(new BCryptPasswordEncoder().matches("Password123", savedUser.getPassword()));
        assertTrue(savedUser.getEmail().equals("arun@example.com"));
        assertTrue(savedUser.getRole() == UserRole.CUSTOMER);
    }

    @Test
    void shouldRejectDuplicateEmail() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        Mockito.when(userRepository.existsByEmailIgnoreCase("arun@example.com")).thenReturn(true);
        AuthService service = service(userRepository, Mockito.mock(JwtService.class));

        assertThrows(EmailAlreadyRegisteredException.class, () -> service.register(
                new RegisterRequest("Arun", "arun@example.com", "Password123")
        ));
    }

    @Test
    void shouldLoginWithCorrectPassword() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        String encodedPassword = new BCryptPasswordEncoder().encode("Password123");
        AppUser user = new AppUser("Arun", "arun@example.com", encodedPassword, UserRole.CUSTOMER);
        Mockito.when(userRepository.findByEmailIgnoreCase("arun@example.com")).thenReturn(java.util.Optional.of(user));
        JwtService jwtService = Mockito.mock(JwtService.class);
        Mockito.when(jwtService.generateToken(user)).thenReturn("test-token");
        AuthService service = service(userRepository, jwtService);

        AuthResponse response = service.login(new LoginRequest("arun@example.com", "Password123"));

        assertTrue(response.email().equals("arun@example.com"));
        assertTrue(response.token().equals("test-token"));
    }

    @Test
    void shouldRejectWrongPassword() {
        UserRepository userRepository = Mockito.mock(UserRepository.class);
        String encodedPassword = new BCryptPasswordEncoder().encode("Password123");
        AppUser user = new AppUser("Arun", "arun@example.com", encodedPassword, UserRole.CUSTOMER);
        Mockito.when(userRepository.findByEmailIgnoreCase("arun@example.com")).thenReturn(java.util.Optional.of(user));
        AuthService service = service(userRepository, Mockito.mock(JwtService.class));

        assertThrows(InvalidCredentialsException.class, () -> service.login(
                new LoginRequest("arun@example.com", "WrongPassword")
        ));
    }
}
