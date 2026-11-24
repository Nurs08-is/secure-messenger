package kz.securemessenger.server.repository;

import kz.securemessenger.server.model.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMemberRepository
        extends JpaRepository<ChatMember, Long> {

    List<ChatMember> findByChat_Id(Long chatId);

    List<ChatMember> findByUser_Id(Long userId);

    boolean existsByChat_IdAndUser_Id(
            Long chatId, Long userId);

    Optional<ChatMember> findByChat_IdAndUser_Id(
            Long chatId, Long userId);

    void deleteByChat_IdAndUser_Id(
            Long chatId, Long userId);
}