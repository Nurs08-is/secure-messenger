package kz.securemessenger.server.service;

import kz.securemessenger.server.model.AuditLog;
import kz.securemessenger.server.repository.AuditLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(
            AuditLogRepository auditLogRepository) {

        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(String username,
                    String action,
                    String details) {

        auditLogRepository.save(
                new AuditLog(
                        username,
                        action,
                        details
                ));
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW)
    public void logFailedLogin(String username) {

        String safeUsername =
                username == null || username.isBlank()
                        ? "UNKNOWN"
                        : username.trim();

        if (safeUsername.length() > 200) {
            safeUsername = safeUsername.substring(0, 200);
        }

        auditLogRepository.save(
                new AuditLog(
                        safeUsername,
                        "LOGIN_FAILED",
                        "Authentication failed"
                ));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAllLogs() {

        return auditLogRepository
                .findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getUserLogs(String username) {

        return auditLogRepository
                .findByUsernameOrderByCreatedAtDesc(
                        username);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getActionLogs(String action) {

        return auditLogRepository
                .findByActionOrderByCreatedAtDesc(
                        action);
    }
}