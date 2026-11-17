
package kz.securemessenger.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

public class AesGcmService {

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final SecureRandom random = new SecureRandom();

    // AES-256 құпия кілтін жасау
    public SecretKey generateKey() throws GeneralSecurityException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256, random);
        return generator.generateKey();
    }

    // Хабарламаны шифрлау
    public EncryptedData encrypt(SecretKey key, String message)
            throws GeneralSecurityException {

        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key,
                new GCMParameterSpec(TAG_LENGTH, iv));

        byte[] ciphertext = cipher.doFinal(
                message.getBytes(StandardCharsets.UTF_8));

        return new EncryptedData(iv, ciphertext);
    }

    // Хабарламаны қайта ашу
    public String decrypt(SecretKey key, EncryptedData data)
            throws GeneralSecurityException {

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key,
                new GCMParameterSpec(TAG_LENGTH, data.iv()));

        byte[] plaintext = cipher.doFinal(data.ciphertext());

        return new String(plaintext, StandardCharsets.UTF_8);
    }

    public record EncryptedData(byte[] iv, byte[] ciphertext) {
    }
}
