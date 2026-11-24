package kz.securemessenger.server.repository;

import kz.securemessenger.server.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByCreatedAtDesc();

    List<AuditLog> findByUsernameOrderByCreatedAtDesc(
            String username);

    List<AuditLog> findByActionOrderByCreatedAtDesc(
            String action);
}