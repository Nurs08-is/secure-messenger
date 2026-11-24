package kz.securemessenger.crypto;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class HybridCryptoServiceTest {

    private final HybridCryptoService crypto =
            new HybridCryptoService();

    private final RsaKeyService rsa =
            new RsaKeyService();

    @Test
    void senderAndRecipientCanDecrypt() throws Exception {

        KeyPair sender = rsa.generateKeyPair();
        KeyPair recipient = rsa.generateKeyPair();

        String text = "Salem, qupia habarlama!";

        EncryptedMessage encrypted = crypto.encrypt(
                text,
                Map.of(
                        1L, sender.getPublic(),
                        2L, recipient.getPublic()
                )
        );

        String senderText = crypto.decrypt(
                encrypted.ciphertext(),
                encrypted.iv(),
                encrypted.encryptedKeys().get(1L),
                sender.getPrivate()
        );

        String recipientText = crypto.decrypt(
                encrypted.ciphertext(),
                encrypted.iv(),
                encrypted.encryptedKeys().get(2L),
                recipient.getPrivate()
        );

        assertEquals(text, senderText);
        assertEquals(text, recipientText);
        assertEquals(2, encrypted.encryptedKeys().size());
    }

    @Test
    void wrongPrivateKeyShouldFail() throws Exception {

        KeyPair recipient = rsa.generateKeyPair();
        KeyPair stranger = rsa.generateKeyPair();

        EncryptedMessage encrypted = crypto.encrypt(
                "Secret message",
                Map.of(1L, recipient.getPublic())
        );

        assertThrows(
                GeneralSecurityException.class,
                () -> crypto.decrypt(
                        encrypted.ciphertext(),
                        encrypted.iv(),
                        encrypted.encryptedKeys().get(1L),
                        stranger.getPrivate()
                )
        );
    }

    @Test
    void tamperedCiphertextShouldFail() throws Exception {

        KeyPair recipient = rsa.generateKeyPair();

        EncryptedMessage encrypted = crypto.encrypt(
                "Original message",
                Map.of(1L, recipient.getPublic())
        );

        byte[] modified = Base64.getDecoder()
                .decode(encrypted.ciphertext());

        modified[0] ^= 1;

        String tampered = Base64.getEncoder()
                .encodeToString(modified);

        assertThrows(
                GeneralSecurityException.class,
                () -> crypto.decrypt(
                        tampered,
                        encrypted.iv(),
                        encrypted.encryptedKeys().get(1L),
                        recipient.getPrivate()
                )
        );
    }

    @Test
    void sameMessageShouldEncryptDifferently()
            throws Exception {

        KeyPair recipient = rsa.generateKeyPair();

        var recipients = Map.of(
                1L, recipient.getPublic());

        EncryptedMessage first = crypto.encrypt(
                "Same message", recipients);

        EncryptedMessage second = crypto.encrypt(
                "Same message", recipients);

        assertNotEquals(first.iv(), second.iv());

        assertNotEquals(
                first.ciphertext(),
                second.ciphertext()
        );
    }

    @Test
    void emptyRecipientsShouldFail() {

        assertThrows(
                IllegalArgumentException.class,
                () -> crypto.encrypt(
                        "Secret message",
                        Map.of()
                )
        );
    }
}