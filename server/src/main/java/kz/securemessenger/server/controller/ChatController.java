package kz.securemessenger.server.controller;

import kz.securemessenger.server.service.ChatService;
import kz.securemessenger.server.service.ChatService.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chats")
@PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','HR','ADMIN')")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatResponse createChat(
            @RequestBody CreateChatRequest request,
            Principal principal) {

        return chatService.createChat(
                principal.getName(),
                request
        );
    }

    @GetMapping
    public List<ChatResponse> getChats(
            Principal principal) {

        return chatService.getChats(
                principal.getName()
        );
    }

    @GetMapping("/{chatId}/members")
    public List<MemberResponse> getMembers(
            @PathVariable Long chatId,
            Principal principal) {

        return chatService.getMembers(
                chatId,
                principal.getName()
        );
    }

    @PostMapping("/{chatId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(
            @PathVariable Long chatId,
            @RequestBody SendMessageRequest request,
            Principal principal) {

        return chatService.sendMessage(
                chatId,
                principal.getName(),
                request
        );
    }

    @GetMapping("/{chatId}/messages")
    public List<MessageResponse> getMessages(
            @PathVariable Long chatId,
            Principal principal) {

        return chatService.getMessages(
                chatId,
                principal.getName()
        );
    }
    @PostMapping("/{chatId}/members/{userId}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void addMember(
        @PathVariable Long chatId,
        @PathVariable Long userId,
        Principal principal) {

    chatService.addMember(
            chatId,
            userId,
            principal.getName()
    );
}

@DeleteMapping("/{chatId}/members/{userId}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void removeMember(
        @PathVariable Long chatId,
        @PathVariable Long userId,
        Principal principal) {

    chatService.removeMember(
            chatId,
            userId,
            principal.getName()
    );
}
}