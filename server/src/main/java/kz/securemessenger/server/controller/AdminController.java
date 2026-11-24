package kz.securemessenger.server.controller;

import kz.securemessenger.server.model.User;
import kz.securemessenger.server.repository.UserRepository;
import kz.securemessenger.common.model.Role;
import kz.securemessenger.common.model.UserStatus;
import kz.securemessenger.server.service.AuditService;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public AdminController(
        UserRepository userRepository,
        AuditService auditService) {

    this.userRepository = userRepository;
    this.auditService = auditService;
}

    @GetMapping("/users")
    public List<Map<String, Object>> users() {

        return userRepository.findAll()
                .stream()
                .map(user -> Map.<String, Object>of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "role", user.getRole().name(),
                        "status", user.getStatus().name()
                ))
                .toList();
    }

    @PutMapping("/users/{id}/role")
    @Transactional
    public Map<String, String> changeRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Principal principal) {

        User user = findUser(id);

        checkSelf(user, principal);

        Role role;

        try {
            role = Role.valueOf(request.get("role"));
        } catch (IllegalArgumentException |
                 NullPointerException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid role");
        }

        user.setRole(role);
userRepository.save(user);

auditService.log(
        principal.getName(),
        "ROLE_CHANGED",
        "User: " + user.getUsername()
                + ", New role: " + role.name()
);

return Map.of("message", "Role updated");
    }

    @PutMapping("/users/{id}/block")
    @Transactional
    public Map<String, String> blockUser(
            @PathVariable Long id,
            Principal principal) {

        User user = findUser(id);

        checkSelf(user, principal);

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Account is not active");
        }

        user.setStatus(UserStatus.BLOCKED);
userRepository.save(user);

auditService.log(
        principal.getName(),
        "ACCOUNT_BLOCKED",
        "Blocked user: " + user.getUsername()
);

return Map.of("message", "Account blocked");
    }

    @PutMapping("/users/{id}/unblock")
    @Transactional
    public Map<String, String> unblockUser(
            @PathVariable Long id,
            Principal principal) {

        User user = findUser(id);

        checkSelf(user, principal);

        if (user.getStatus() != UserStatus.BLOCKED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Account is not blocked");
        }

        user.setStatus(UserStatus.ACTIVE);
userRepository.save(user);

auditService.log(
        principal.getName(),
        "ACCOUNT_UNBLOCKED",
        "Unblocked user: " + user.getUsername()
);

return Map.of("message", "Account unblocked");
    }

    private User findUser(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"));
    }

    private void checkSelf(User user, Principal principal) {

        if (user.getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cannot modify your own account");
        }
    }
}