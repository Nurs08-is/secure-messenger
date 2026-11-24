package kz.securemessenger.server.config;

import kz.securemessenger.server.service.JwtService;
import kz.securemessenger.server.repository.UserRepository;
import kz.securemessenger.common.model.UserStatus;
import kz.securemessenger.common.model.Role;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import io.jsonwebtoken.JwtException;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public WebSocketConfig(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry config) {

        config.enableSimpleBroker("/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry) {

        registry.addEndpoint("/ws");
    }

    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration) {

        registration.interceptors(new ChannelInterceptor() {

            @Override
            public Message<?> preSend(
                    Message<?> message,
                    MessageChannel channel) {

                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(
                                message,
                                StompHeaderAccessor.class);

                if (accessor == null ||
                        accessor.getCommand() == null) {
                    return message;
                }

                StompCommand command = accessor.getCommand();

                if (command == StompCommand.CONNECT ||
                        command == StompCommand.STOMP) {

                    String header = accessor
                            .getFirstNativeHeader("Authorization");

                    if (header == null ||
                            !header.startsWith("Bearer ")) {

                        throw new AccessDeniedException(
                                "JWT token required");
                    }

                    String username;

                    try {
                        username = jwtService
                                .validateToken(header.substring(7))
                                .getSubject();

                    } catch (JwtException |
                             IllegalArgumentException e) {

                        throw new AccessDeniedException(
                                "Invalid JWT token");
                    }

                    var user = userRepository
                            .findByUsername(username)
                            .filter(u ->
                                    u.getStatus() == UserStatus.ACTIVE &&
                                    u.getRole() != Role.AUDITOR)
                            .orElseThrow(() ->
                                    new AccessDeniedException(
                                            "Access denied"));

                    var authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user.getUsername(),
                                    null,
                                    List.of(
                                            new SimpleGrantedAuthority(
                                                    "ROLE_" +
                                                    user.getRole().name())
                                    )
                            );

                    accessor.setUser(authentication);

                } else if (command == StompCommand.SUBSCRIBE) {

                    if (accessor.getUser() == null ||
                            !"/user/queue/messages".equals(
                                    accessor.getDestination())) {

                        throw new AccessDeniedException(
                                "Subscription denied");
                    }

                    boolean active = userRepository
                            .findByUsername(
                                    accessor.getUser().getName())
                            .filter(u ->
                                    u.getStatus() == UserStatus.ACTIVE &&
                                    u.getRole() != Role.AUDITOR)
                            .isPresent();

                    if (!active) {
                        throw new AccessDeniedException(
                                "Account unavailable");
                    }

                } else if (command != StompCommand.DISCONNECT &&
                           command != StompCommand.UNSUBSCRIBE) {

                    throw new AccessDeniedException(
                            "Operation denied");
                }

                return message;
            }
        });
    }
}