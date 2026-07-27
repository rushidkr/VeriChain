package com.verichain.service;

import com.verichain.dto.request.LoginRequest;
import com.verichain.dto.request.RegisterRequest;
import com.verichain.dto.response.AuthResponse;
import com.verichain.entity.ApprovalStatus;
import com.verichain.entity.IssuerProfile;
import com.verichain.entity.Role;
import com.verichain.entity.User;
import com.verichain.exception.VerichainException;
import com.verichain.repository.IssuerProfileRepository;
import com.verichain.repository.UserRepository;
import com.verichain.security.JwtUtil;
import com.verichain.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final IssuerProfileRepository issuerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse registerStudent(RegisterRequest request) {
        User user = createBaseUser(request, Role.STUDENT);
        String token = jwtUtil.generateToken(new UserPrincipal(user), user.getId(), user.getRole().name());
        return toAuthResponse(user, token, "Registration successful");
    }

    @Transactional
    public AuthResponse registerIssuer(RegisterRequest request) {
        if (request.getOrganizationName() == null || request.getOrganizationName().isBlank()) {
            throw new VerichainException("Organization name is required for issuer registration", HttpStatus.BAD_REQUEST);
        }

        User user = createBaseUser(request, Role.ISSUER);

        IssuerProfile profile = IssuerProfile.builder()
                .user(user)
                .organizationName(request.getOrganizationName())
                .registrationNumber(request.getRegistrationNumber())
                .approvalStatus(ApprovalStatus.PENDING)
                .build();
        issuerProfileRepository.save(profile);

        String token = jwtUtil.generateToken(new UserPrincipal(user), user.getId(), user.getRole().name());
        return toAuthResponse(user, token,
                "Registration successful - your account is pending admin approval before you can issue credentials");
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new VerichainException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        String token = jwtUtil.generateToken(new UserPrincipal(user), user.getId(), user.getRole().name());
        return toAuthResponse(user, token, "Login successful");
    }

    private User createBaseUser(RegisterRequest request, Role role) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new VerichainException("An account with this email already exists", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        return userRepository.save(user);
    }

    private AuthResponse toAuthResponse(User user, String token, String message) {
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .message(message)
                .build();
    }
}
