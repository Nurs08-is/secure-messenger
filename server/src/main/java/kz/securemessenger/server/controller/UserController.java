package kz.securemessenger.server.controller;

import kz.securemessenger.server.model.User;
import kz.securemessenger.server.repository.UserRepository;
import kz.securemessenger.common.model.UserStatus;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserResponse currentUser(Principal principal) {

        return toResponse(findCurrentUser(principal));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','HR','ADMIN')")
    public List<UserResponse> users() {

        return userRepository.findByStatus(UserStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/department")
    @PreAuthorize("hasRole('MANAGER')")
    public List<UserResponse> departmentUsers(
            Principal principal) {

        User manager = findCurrentUser(principal);

        if (manager.getDepartment() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Manager has no department");
        }

        return userRepository
                .findByDepartment_IdAndStatus(
                        manager.getDepartment().getId(),
                        UserStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private User findCurrentUser(Principal principal) {

        return userRepository
                .findByUsername(principal.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"));
    }

    private UserResponse toResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getPublicKey(),
                user.getDepartment() == null
                        ? null
                        : user.getDepartment().getName()
        );
    }

    public record UserResponse(
            Long id,
            String username,
            String role,
            String status,
            String publicKey,
            String department
    ) {}
}