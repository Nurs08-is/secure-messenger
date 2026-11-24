package kz.securemessenger.server.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatType type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Chat() {
    }

    public Chat(String name, ChatType type,
                User createdBy, Department department) {

        this.name = name;
        this.type = type;
        this.createdBy = createdBy;
        this.department = department;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ChatType getType() {
        return type;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Department getDepartment() {
        return department;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public enum ChatType {
        PRIVATE,
        GROUP,
        DEPARTMENT
    }
}