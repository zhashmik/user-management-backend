package com.syn.usermanagement.controller;

import com.syn.usermanagement.dto.LoginRequest;
import com.syn.usermanagement.dto.LoginResponse;
import com.syn.usermanagement.dto.RegisterRequest;
import com.syn.usermanagement.entity.User;
import com.syn.usermanagement.repository.UserRepository;
import com.syn.usermanagement.security.CustomUserDetailsService;
import com.syn.usermanagement.security.JwtUtils;
import com.syn.usermanagement.service.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    // Create logger for this class
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request
    ) {
        String clientIp = getClientIp(request);

        logger.info("🔐 LOGIN ATTEMPT - Email: {} - IP: {}",
                loginRequest.getEmail(), clientIp);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userDetailsService.loadUserEntityByEmail(loginRequest.getEmail());
            String token = jwtUtils.generateToken(userDetails);

            logger.info("✅ LOGIN SUCCESS - Email: {} - UserId: {} - IP: {}",
                    user.getEmail(), user.getId(), clientIp);

            LoginResponse response = new LoginResponse(
                    token,
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole().name()
            );

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            logger.warn("❌ LOGIN FAILED - Email: {} - Reason: Invalid credentials - IP: {}",
                    loginRequest.getEmail(), clientIp);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        } catch (Exception e) {
            logger.error("🔥 LOGIN ERROR - Email: {} - Error: {} - IP: {}",
                    loginRequest.getEmail(), e.getMessage(), clientIp, e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Login failed"));
        }
    }

    /**
     * Register endpoint
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest registerRequest,
            HttpServletRequest request
    ) {
        String clientIp = getClientIp(request);

        logger.info("📝 REGISTRATION ATTEMPT - Email: {} - IP: {}",
                registerRequest.getEmail(), clientIp);

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            logger.warn("⚠️ REGISTRATION FAILED - Email: {} - Reason: Email already exists - IP: {}",
                    registerRequest.getEmail(), clientIp);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Email already exists"));
        }

        try {
            User user = new User();
            user.setName(registerRequest.getName());
            user.setEmail(registerRequest.getEmail());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setRole(User.Role.USER);

            User savedUser = userRepository.save(user);

            logger.info("✅ REGISTRATION SUCCESS - Email: {} - UserId: {} - IP: {}",
                    savedUser.getEmail(), savedUser.getId(), clientIp);

            UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
            String token = jwtUtils.generateToken(userDetails);

            LoginResponse response = new LoginResponse(
                    token,
                    savedUser.getId(),
                    savedUser.getName(),
                    savedUser.getEmail(),
                    savedUser.getRole().name()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("🔥 REGISTRATION ERROR - Email: {} - Error: {} - IP: {}",
                    registerRequest.getEmail(), e.getMessage(), clientIp, e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Registration failed"));
        }
    }

    /**
     * Logout endpoint
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request
    ) {
        String clientIp = getClientIp(request);

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("⚠️ LOGOUT FAILED - Reason: Invalid auth header - IP: {}", clientIp);
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Invalid authorization header"));
            }

            String token = authHeader.substring(7);
            String userEmail = jwtUtils.extractUsername(token);

            tokenBlacklistService.blacklistToken(token);

            logger.info("🚪 LOGOUT SUCCESS - Email: {} - IP: {}", userEmail, clientIp);

            return ResponseEntity.ok(Map.of(
                    "message", "Logged out successfully",
                    "success", true
            ));

        } catch (Exception e) {
            logger.error("🔥 LOGOUT ERROR - Error: {} - IP: {}", e.getMessage(), clientIp, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Logout failed: " + e.getMessage()));
        }
    }

    /**
     * Get client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}