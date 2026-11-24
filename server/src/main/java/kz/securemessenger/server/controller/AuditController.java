package kz.securemessenger.server.controller;

import kz.securemessenger.server.model.AuditLog;
import kz.securemessenger.server.service.AuditService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('AUDITOR')")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<AuditLog> getAllLogs() {

        return auditService.getAllLogs();
    }

    @GetMapping("/user/{username}")
    public List<AuditLog> getUserLogs(
            @PathVariable String username) {

        return auditService.getUserLogs(username);
    }

    @GetMapping("/action/{action}")
    public List<AuditLog> getActionLogs(
            @PathVariable String action) {

        return auditService.getActionLogs(action);
    }
}