package kz.securemessenger.server.repository;

import kz.securemessenger.server.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository
        extends JpaRepository<Message, Long> {

    List<Message> findByChat_IdOrderByCreatedAtAsc(
            Long chatId);

    List<Message> findBySender_Id(Long senderId);

    long countByChat_Id(Long chatId);
}