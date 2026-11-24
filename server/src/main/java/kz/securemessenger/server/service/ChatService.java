package kz.securemessenger.server.service;

import kz.securemessenger.server.model.*;
import kz.securemessenger.server.repository.*;
import kz.securemessenger.common.model.Role;
import kz.securemessenger.common.model.UserStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final MessageKeyRepository keyRepository;
    private final UserRepository userRepository;
    private final MessageNotificationService notificationService;
    private final AuditService auditService;

    public ChatService(
            ChatRepository chatRepository,
            ChatMemberRepository memberRepository,
            MessageRepository messageRepository,
            MessageKeyRepository keyRepository,
            UserRepository userRepository,
MessageNotificationService notificationService,
AuditService auditService) {

        this.chatRepository = chatRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.keyRepository = keyRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    public ChatResponse createChat(
            String username, CreateChatRequest request) {

        User creator = findUser(username);

        if (request.name() == null ||
                request.name().isBlank() ||
                request.name().length() > 200 ||
                request.type() == null) {
            throw badRequest("Invalid chat name or type");
        }

        Set<Long> ids = new HashSet<>();

        if (request.memberIds() != null) {
            ids.addAll(request.memberIds());
        }

        ids.add(creator.getId());

        Department department = null;

        if (request.type() == Chat.ChatType.PRIVATE
                && ids.size() != 2) {
            throw badRequest(
                    "Private chat requires two users");
        }

        if (request.type() == Chat.ChatType.GROUP
                && ids.size() < 3) {
            throw badRequest(
                    "Group chat requires at least three users");
        }

        if (request.type() == Chat.ChatType.DEPARTMENT) {

            if (creator.getRole() != Role.MANAGER ||
                    creator.getDepartment() == null) {
                throw forbidden(
                        "Only department manager can create this chat");
            }

            department = creator.getDepartment();
        }

        List<User> members = new ArrayList<>();

        for (Long id : ids) {

            if (id == null) {
                throw badRequest("Invalid member ID");
            }

            User member = userRepository.findById(id)
                    .orElseThrow(() ->
                            badRequest("Member not found"));

            if (member.getStatus() != UserStatus.ACTIVE ||
                    member.getRole() == Role.AUDITOR ||
                    member.getPublicKey() == null ||
                    member.getPublicKey().isBlank()) {
                throw badRequest(
                        "Member is unavailable: "
                                + member.getUsername());
            }

            if (department != null &&
                    (member.getDepartment() == null ||
                    !department.getId().equals(
                            member.getDepartment().getId()))) {

                throw forbidden(
                        "Member belongs to another department");
            }

            members.add(member);
        }

        Chat chat = chatRepository.save(
                new Chat(
                        request.name().trim(),
                        request.type(),
                        creator,
                        department
                ));

        for (User member : members) {
    memberRepository.save(
            new ChatMember(chat, member));
}

auditService.log(
        creator.getUsername(),
        "CHAT_CREATED",
        "Chat ID: " + chat.getId()
                + ", Type: " + chat.getType().name()
);

return toResponse(chat);
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> getChats(String username) {

        User user = findUser(username);

        return memberRepository.findByUser_Id(user.getId())
                .stream()
                .map(ChatMember::getChat)
                .filter(chat -> canAccess(chat, user))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(
            Long chatId, String username) {

        User user = findUser(username);

        Chat chat = requireMembership(chatId, user);

        return activeMembers(chat).stream()
                .map(member -> new MemberResponse(
                        member.getId(),
                        member.getUsername(),
                        member.getPublicKey()
                ))
                .toList();
    }

    public MessageResponse sendMessage(
            Long chatId,
            String username,
            SendMessageRequest request) {

        User sender = findUser(username);

        Chat chat = requireMembership(chatId, sender);

        validateBase64(request.iv(), 12, 12);

        validateBase64(
                request.ciphertext(), 16, 65536);

        List<User> members = activeMembers(chat);

        Set<Long> expectedIds = new HashSet<>();

        for (User member : members) {
            expectedIds.add(member.getId());
        }

        if (request.encryptedKeys() == null ||
                !expectedIds.equals(
                        request.encryptedKeys().keySet())) {

            throw badRequest(
                    "Encrypted keys required for all active members");
        }

        for (String key :
                request.encryptedKeys().values()) {

            validateBase64(key, 256, 256);
        }

        Message message = messageRepository.save(
                new Message(
                        chat,
                        sender,
                        request.ciphertext(),
                        request.iv()
                ));

        for (User member : members) {

            String encryptedKey =
                    request.encryptedKeys()
                            .get(member.getId());

            keyRepository.save(
                    new MessageKey(
                            message,
                            member,
                            encryptedKey
                    ));
        }

        notificationService.notifyMembers(
        chat.getId(),
        message.getId(),
        members.stream()
                .map(User::getUsername)
                .toList()
);

return toMessageResponse(
        message,
        request.encryptedKeys().get(sender.getId())
);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(
            Long chatId, String username) {

        User user = findUser(username);

        requireMembership(chatId, user);

        List<MessageResponse> result = new ArrayList<>();

        for (Message message :
                messageRepository
                    .findByChat_IdOrderByCreatedAtAsc(chatId)) {

            keyRepository
                    .findByMessage_IdAndUser_Id(
                            message.getId(), user.getId())
                    .ifPresent(key ->
                            result.add(toMessageResponse(
                                    message,
                                    key.getEncryptedKey()
                            )));
        }

        return result;
    }
public void addMember(Long chatId, Long userId,
                      String username) {

    User actor = findUser(username);
    Chat chat = requireMembership(chatId, actor);

    checkMemberManagement(chat, actor);

    User member = userRepository.findById(userId)
            .orElseThrow(() ->
                    badRequest("User not found"));

    if (!canAccess(chat, member) ||
            member.getPublicKey() == null ||
            member.getPublicKey().isBlank()) {

        throw badRequest("User is unavailable");
    }

    if (memberRepository.existsByChat_IdAndUser_Id(
            chatId, userId)) {

        throw badRequest("User is already a member");
    }

    memberRepository.save(new ChatMember(chat, member));

    auditService.log(
            username,
            "MEMBER_ADDED",
            "Chat ID: " + chatId + ", User ID: " + userId
    );
}

public void removeMember(Long chatId, Long userId,
                         String username) {

    User actor = findUser(username);
    Chat chat = requireMembership(chatId, actor);

    checkMemberManagement(chat, actor);

    if (chat.getCreatedBy().getId().equals(userId) ||
            actor.getId().equals(userId)) {

        throw badRequest("Cannot remove this user");
    }

    if (!memberRepository.existsByChat_IdAndUser_Id(
            chatId, userId)) {

        throw badRequest("User is not a member");
    }

    memberRepository.deleteByChat_IdAndUser_Id(
            chatId, userId);

    auditService.log(
            username,
            "MEMBER_REMOVED",
            "Chat ID: " + chatId + ", User ID: " + userId
    );
}

private void checkMemberManagement(Chat chat,
                                   User actor) {

    if (chat.getType() == Chat.ChatType.PRIVATE) {
        throw forbidden(
                "Cannot modify private chat members");
    }

    if (chat.getType() == Chat.ChatType.DEPARTMENT) {

        if (actor.getRole() != Role.MANAGER ||
                actor.getDepartment() == null ||
                !actor.getDepartment().getId().equals(
                        chat.getDepartment().getId())) {

            throw forbidden("Access denied");
        }

    } else if (!chat.getCreatedBy().getId().equals(
            actor.getId())) {

        throw forbidden("Only chat creator can manage members");
    }
}
    private Chat requireMembership(
            Long chatId, User user) {

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Chat not found"));

        if (!canAccess(chat, user) ||
                !memberRepository
                        .existsByChat_IdAndUser_Id(
                                chatId, user.getId())) {

            throw forbidden("Access denied");
        }

        return chat;
    }

    private boolean canAccess(Chat chat, User user) {

        if (user.getStatus() != UserStatus.ACTIVE ||
                user.getRole() == Role.AUDITOR) {
            return false;
        }

        if (chat.getType() == Chat.ChatType.DEPARTMENT) {
            return user.getDepartment() != null &&
                    chat.getDepartment() != null &&
                    user.getDepartment().getId().equals(
                            chat.getDepartment().getId());
        }

        return true;
    }

    private List<User> activeMembers(Chat chat) {

        return memberRepository.findByChat_Id(chat.getId())
                .stream()
                .map(ChatMember::getUser)
                .filter(user -> canAccess(chat, user))
                .toList();
    }

    private User findUser(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        forbidden("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE ||
                user.getRole() == Role.AUDITOR) {
            throw forbidden("Chat access denied");
        }

        return user;
    }

    private void validateBase64(
            String value, int min, int max) {

        if (value == null ||
                value.length() > 4L * ((max + 2L) / 3L)) {
            throw badRequest("Invalid encrypted data");
        }

        byte[] decoded;

        try {
            decoded = Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw badRequest("Invalid Base64");
        }

        if (decoded.length < min ||
                decoded.length > max) {

            throw badRequest("Invalid encrypted data size");
        }
    }

    private ChatResponse toResponse(Chat chat) {

        return new ChatResponse(
                chat.getId(),
                chat.getName(),
                chat.getType().name(),
                chat.getCreatedAt()
        );
    }

    private MessageResponse toMessageResponse(
            Message message, String encryptedKey) {

        return new MessageResponse(
                message.getId(),
                message.getChat().getId(),
                message.getSender().getUsername(),
                message.getCiphertext(),
                message.getIv(),
                encryptedKey,
                message.getCreatedAt()
        );
    }

    private ResponseStatusException badRequest(
            String message) {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException forbidden(
            String message) {

        return new ResponseStatusException(
                HttpStatus.FORBIDDEN, message);
    }

    public record CreateChatRequest(
            String name,
            Chat.ChatType type,
            List<Long> memberIds
    ) {}

    public record SendMessageRequest(
            String ciphertext,
            String iv,
            Map<Long, String> encryptedKeys
    ) {}

    public record ChatResponse(
            Long id,
            String name,
            String type,
            Instant createdAt
    ) {}

    public record MemberResponse(
            Long id,
            String username,
            String publicKey
    ) {}

    public record MessageResponse(
            Long id,
            Long chatId,
            String sender,
            String ciphertext,
            String iv,
            String encryptedKey,
            Instant createdAt
    ) {}
}