package kz.securemessenger.crypto;

import java.util.Map;

public record EncryptedMessage(
        String ciphertext,
        String iv,
        Map<Long, String> encryptedKeys
) {

    public EncryptedMessage {
        encryptedKeys = Map.copyOf(encryptedKeys);
    }
}