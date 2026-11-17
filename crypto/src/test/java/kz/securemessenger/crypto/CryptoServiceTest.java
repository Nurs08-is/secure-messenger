
package kz.securemessenger.crypto;

import org.junit.jupiter.api.Test;
import javax.crypto.SecretKey;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceTest {

    @Test
    void aesEncryptDecryptTest() throws Exception {
        AesGcmService aes = new AesGcmService();
        SecretKey key = aes.generateKey();

        String message = "Salem, bul qupia habar!";
        var encrypted = aes.encrypt(key, message);
        String decrypted = aes.decrypt(key, encrypted);

        assertEquals(message, decrypted);
    }

    @Test
    void rsaWrapUnwrapTest() throws Exception {
        AesGcmService aes = new AesGcmService();
        RsaKeyService rsa = new RsaKeyService();

        SecretKey aesKey = aes.generateKey();
        KeyPair keys = rsa.generateKeyPair();

        byte[] wrapped = rsa.wrapKey(aesKey, keys.getPublic());
        SecretKey restored = rsa.unwrapKey(
                wrapped, keys.getPrivate());

        assertArrayEquals(
                aesKey.getEncoded(),
                restored.getEncoded());
    }

    @Test
    void tamperedMessageShouldFail() throws Exception {
        AesGcmService aes = new AesGcmService();
        SecretKey key = aes.generateKey();

        var encrypted = aes.encrypt(key, "Qupia habar");
        byte[] changed = encrypted.ciphertext().clone();
        changed[0] ^= 1;

        var tampered = new AesGcmService.EncryptedData(
                encrypted.iv(), changed);

        assertThrows(
                java.security.GeneralSecurityException.class,
                () -> aes.decrypt(key, tampered));
    }
}
