package com.parking.usermgmt.internal;

import com.parking.usermgmt.User;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Reads the {@code Authorization: Bearer <token>} header, validates the JWT,
 * and populates the {@link SecurityContextHolder} with the authenticated principal.
 *
 * <p>The principal stored in the {@code Authentication} is the user's {@link UUID}.
 * Requests without a valid token proceed unauthenticated (the security filter chain
 * will reject them for protected endpoints).
 */
@Component
class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.parseToken(token);
                UUID userId = UUID.fromString(claims.getSubject());
                User.Role role = User.Role.valueOf(claims.get("role", String.class));
                var auth = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                // Invalid / expired token — continue as unauthenticated
            }
        }
        chain.doFilter(request, response);
    }
}
