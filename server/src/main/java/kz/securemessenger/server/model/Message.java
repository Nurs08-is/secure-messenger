package kz.securemessenger.server.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String ciphertext;

    @Column(nullable = false, length = 32)
    private String iv;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Message() {
    }

    public Message(Chat chat, User sender,
                   String ciphertext, String iv) {

        this.chat = chat;
        this.sender = sender;
        this.ciphertext = ciphertext;
        this.iv = iv;
    }

    public Long getId() {
        return id;
    }

    public Chat getChat() {
        return chat;
    }

    public User getSender() {
        return sender;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public String getIv() {
        return iv;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}