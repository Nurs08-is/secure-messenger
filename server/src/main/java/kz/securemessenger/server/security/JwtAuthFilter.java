package kz.securemessenger.server.security;

import kz.securemessenger.server.service.JwtService;
import kz.securemessenger.server.repository.UserRepository;
import kz.securemessenger.common.model.UserStatus;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService,
                         UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);

            try {
                String username = jwtService
                        .validateToken(token)
                        .getSubject();

                userRepository.findByUsername(username)
                        .filter(user ->
                                user.getStatus() == UserStatus.ACTIVE)
                        .ifPresent(user -> {

                            var authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            user.getUsername(),
                                            null,
                                            List.of(
                                                    new SimpleGrantedAuthority(
                                                            "ROLE_" + user.getRole().name()
                                                    )
                                            )
                                    );

                            SecurityContextHolder.getContext()
                                    .setAuthentication(authentication);
                        });

            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}