package kz.securemessenger.server.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "chat_members",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"chat_id", "user_id"}
       ))
public class ChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Instant joinedAt = Instant.now();

    protected ChatMember() {
    }

    public ChatMember(Chat chat, User user) {
        this.chat = chat;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public Chat getChat() {
        return chat;
    }

    public User getUser() {
        return user;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}