package kz.securemessenger.server.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class MessageNotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    MessageNotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public MessageNotificationService(
            SimpMessagingTemplate messagingTemplate,
            ApplicationEventPublisher eventPublisher) {

        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
    }

    public void notifyMembers(
            Long chatId,
            Long messageId,
            List<String> usernames) {

        eventPublisher.publishEvent(
                new MessageEvent(
                        chatId,
                        messageId,
                        List.copyOf(usernames)
                ));
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT)
    public void sendNotification(MessageEvent event) {

        Notification notification = new Notification(
                "NEW_MESSAGE",
                event.chatId(),
                event.messageId()
        );

        for (String username : event.usernames()) {

            try {
                messagingTemplate.convertAndSendToUser(
                        username,
                        "/queue/messages",
                        notification
                );
            } catch (RuntimeException e) {
                log.warn("Message notification failed");
            }
        }
    }

    public record MessageEvent(
            Long chatId,
            Long messageId,
            List<String> usernames
    ) {}

    public record Notification(
            String type,
            Long chatId,
            Long messageId
    ) {}
}