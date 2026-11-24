package kz.securemessenger.server.repository;

import kz.securemessenger.server.model.MessageKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageKeyRepository
        extends JpaRepository<MessageKey, Long> {

    List<MessageKey> findByMessage_Id(Long messageId);

    List<MessageKey> findByUser_Id(Long userId);

    Optional<MessageKey> findByMessage_IdAndUser_Id(
            Long messageId, Long userId);
}