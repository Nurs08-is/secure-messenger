package kz.securemessenger.server.repository;

import kz.securemessenger.server.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository
        extends JpaRepository<Chat, Long> {

    List<Chat> findByCreatedBy_Id(Long userId);

    List<Chat> findByDepartment_Id(Long departmentId);

    List<Chat> findByType(Chat.ChatType type);
}