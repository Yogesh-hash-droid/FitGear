package com.fitgear.backend.config;

import com.fitgear.backend.auth.AppUser;
import com.fitgear.backend.auth.UserRepository;
import com.fitgear.backend.auth.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Provisions the admin account from environment variables.
 * Public registration always creates CUSTOMER accounts, so this is the
 * only automated path to an ADMIN - and it requires ADMIN_EMAIL +
 * ADMIN_PASSWORD to be set (never committed to source control).
 */
@Component
public class AdminProvisioner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminProvisioner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminProvisioner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${fitgear.admin.email:}") String adminEmail,
            @Value("${fitgear.admin.password:}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.info("Admin provisioning skipped: ADMIN_EMAIL / ADMIN_PASSWORD not set");
            return;
        }

        String email = adminEmail.trim().toLowerCase();
        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(
                user -> promoteIfNeeded(user),
                () -> {
                    AppUser admin = new AppUser("FitGear Admin", email,
                            passwordEncoder.encode(adminPassword), UserRole.ADMIN);
                    userRepository.save(admin);
                    log.info("Provisioned admin account {}", email);
                }
        );
    }

    private void promoteIfNeeded(AppUser user) {
        if (user.getRole() != UserRole.ADMIN) {
            user.setRole(UserRole.ADMIN);
            userRepository.save(user);
            log.info("Promoted existing account {} to ADMIN", user.getEmail());
        }
    }
}
