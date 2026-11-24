package kz.securemessenger.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.security.KeyPair;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;

class KeyStorageServiceTest {

    @TempDir
    Path tempDir;

    private final KeyStorageService storage =
            new KeyStorageService();

    private final RsaKeyService rsa =
            new RsaKeyService();

    @Test
    void saveAndLoadKeys() throws Exception {

        KeyPair original = rsa.generateKeyPair();

        Path file = tempDir.resolve("user.key");

        char[] password = "SecurePassword123".toCharArray();

        storage.save(file, original, password);

        assertTrue(Files.exists(file));

        KeyPair loaded = storage.load(file, password);

        assertArrayEquals(
                original.getPrivate().getEncoded(),
                loaded.getPrivate().getEncoded()
        );

        assertArrayEquals(
                original.getPublic().getEncoded(),
                loaded.getPublic().getEncoded()
        );
    }

    @Test
    void wrongPasswordShouldFail() throws Exception {

        KeyPair pair = rsa.generateKeyPair();

        Path file = tempDir.resolve("user.key");

        storage.save(
                file,
                pair,
                "CorrectPassword123".toCharArray()
        );

        assertThrows(
                GeneralSecurityException.class,
                () -> storage.load(
                        file,
                        "WrongPassword123".toCharArray()
                )
        );
    }

    @Test
    void tamperedKeyFileShouldFail() throws Exception {

        KeyPair pair = rsa.generateKeyPair();

        Path file = tempDir.resolve("user.key");

        char[] password = "SecurePassword123".toCharArray();

        storage.save(file, pair, password);

        byte[] data = Files.readAllBytes(file);

        data[data.length - 1] ^= 1;

        Files.write(file, data);

        assertThrows(
                GeneralSecurityException.class,
                () -> storage.load(file, password)
        );
    }

    @Test
    void existingKeyShouldNotBeOverwritten()
            throws Exception {

        KeyPair pair = rsa.generateKeyPair();

        Path file = tempDir.resolve("user.key");

        char[] password = "SecurePassword123".toCharArray();

        storage.save(file, pair, password);

        assertThrows(
                FileAlreadyExistsException.class,
                () -> storage.save(file, pair, password)
        );
    }
}