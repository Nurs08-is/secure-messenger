package kz.securemessenger.crypto;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class HybridCryptoService {

    private final AesGcmService aesService =
            new AesGcmService();

    private final RsaKeyService rsaService =
            new RsaKeyService();

    public EncryptedMessage encrypt(
            String message,
            Map<Long, PublicKey> recipients)
            throws GeneralSecurityException {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Message is empty");
        }

        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException(
                    "Recipients are required");
        }

        SecretKey aesKey = aesService.generateKey();

        var encrypted = aesService.encrypt(
                aesKey, message);

        Map<Long, String> encryptedKeys =
                new HashMap<>();

        for (var recipient : recipients.entrySet()) {

            byte[] wrappedKey = rsaService.wrapKey(
                    aesKey,
                    recipient.getValue()
            );

            encryptedKeys.put(
                    recipient.getKey(),
                    Base64.getEncoder()
                            .encodeToString(wrappedKey)
            );
        }

        return new EncryptedMessage(
                Base64.getEncoder()
                        .encodeToString(encrypted.ciphertext()),

                Base64.getEncoder()
                        .encodeToString(encrypted.iv()),

                encryptedKeys
        );
    }

    public String decrypt(
            String ciphertext,
            String iv,
            String encryptedKey,
            PrivateKey privateKey)
            throws GeneralSecurityException {

        Base64.Decoder decoder = Base64.getDecoder();

        SecretKey aesKey = rsaService.unwrapKey(
                decoder.decode(encryptedKey),
                privateKey
        );

        var encryptedData =
                new AesGcmService.EncryptedData(
                        decoder.decode(iv),
                        decoder.decode(ciphertext)
                );

        return aesService.decrypt(
                aesKey, encryptedData);
    }
}