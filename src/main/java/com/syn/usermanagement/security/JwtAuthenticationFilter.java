package com.syn.usermanagement.security;

import com.syn.usermanagement.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Get Authorization header
        final String authHeader = request.getHeader("Authorization");

        // Check if header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token (remove "Bearer " prefix)
        final String jwt = authHeader.substring(7);

        try {
            // ✅ Check if token is blacklisted (logged out)
            if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
                logger.warn("Attempted use of blacklisted token");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"message\": \"Token has been invalidated. Please login again.\"}");
                return;
            }

            // Extract username from token
            final String userEmail = jwtUtils.extractUsername(jwt);

            // If email exists and user is not already authenticated
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Load user details
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                // Validate token
                if (jwtUtils.validateToken(jwt, userDetails)) {

                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    // Set details
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set authentication in context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
