package kz.securemessenger.server.model;

import jakarta.persistence.*;

@Entity
@Table(name = "message_keys",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"message_id", "user_id"}
       ))
public class MessageKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "message_id")
    private Message message;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedKey;

    protected MessageKey() {
    }

    public MessageKey(Message message, User user,
                      String encryptedKey) {

        this.message = message;
        this.user = user;
        this.encryptedKey = encryptedKey;
    }

    public Long getId() {
        return id;
    }

    public Message getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }

    public String getEncryptedKey() {
        return encryptedKey;
    }
}