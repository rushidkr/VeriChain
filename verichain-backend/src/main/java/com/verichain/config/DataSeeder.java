package com.verichain.config;

import com.verichain.entity.Role;
import com.verichain.entity.User;
import com.verichain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * There is deliberately no "/api/auth/register/admin" endpoint - anyone being able to self-register
 * as an admin would defeat the entire point of the issuer-approval workflow. Instead, a single
 * admin account is seeded from configuration/environment on first startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${verichain.admin.email:admin@verichain.com}")
    private String adminEmail;

    @Value("${verichain.admin.password:ChangeMe123!}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        User admin = User.builder()
                .name("VeriChain Admin")
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);
        log.info("Seeded default admin account: {} (change the password after first login!)", adminEmail);
    }
}
