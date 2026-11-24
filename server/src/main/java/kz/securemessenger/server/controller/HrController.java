package kz.securemessenger.server.controller;

import kz.securemessenger.server.model.User;
import kz.securemessenger.server.model.Department;
import kz.securemessenger.server.repository.UserRepository;
import kz.securemessenger.server.repository.DepartmentRepository;
import kz.securemessenger.common.model.UserStatus;
import kz.securemessenger.server.service.AuditService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.security.Principal;

@RestController
@RequestMapping("/api/hr")
@PreAuthorize("hasRole('HR')")
public class HrController {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditService auditService;

    public HrController(
        UserRepository userRepository,
        DepartmentRepository departmentRepository,
        AuditService auditService) {

    this.userRepository = userRepository;
    this.departmentRepository = departmentRepository;
    this.auditService = auditService;
}

    @GetMapping("/pending")
    public List<Map<String, Object>> pendingUsers() {

        return userRepository.findByStatus(UserStatus.PENDING)
                .stream()
                .map(user -> Map.<String, Object>of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "status", user.getStatus().name()
                ))
                .toList();
    }

    @PostMapping("/approve/{id}")
@Transactional
public Map<String, String> approveUser(
        @PathVariable Long id,
        Principal principal) {

    User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found"));

    if (user.getStatus() != UserStatus.PENDING) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Account is not pending");
    }

    user.setStatus(UserStatus.ACTIVE);
    userRepository.save(user);

    auditService.log(
            principal.getName(),
            "ACCOUNT_APPROVED",
            "Approved user: " + user.getUsername()
    );

    return Map.of("message", "Account approved");
}

    @GetMapping("/departments")
    public List<Department> departments() {
        return departmentRepository.findAll();
    }

    @PostMapping("/departments")
    public Department createDepartment(
            @RequestBody Map<String, String> request) {

        String name = request.get("name");

        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Department name is required");
        }

        name = name.trim();

        if (departmentRepository.existsByName(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Department already exists");
        }

        return departmentRepository.save(
                new Department(name));
    }

    @PutMapping("/users/{id}/department/{departmentId}")
    @Transactional
    public Map<String, String> assignDepartment(
        @PathVariable Long id,
        @PathVariable Long departmentId,
        Principal principal) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        Department department = departmentRepository
                .findById(departmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Department not found"));

        user.setDepartment(department);
userRepository.save(user);

auditService.log(
        principal.getName(),
        "DEPARTMENT_ASSIGNED",
        "User: " + user.getUsername()
                + ", Department: " + department.getName()
);

return Map.of("message", "Department assigned");
    }
}