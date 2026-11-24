package kz.securemessenger.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

public class KeyStorageService {

    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final int ITERATIONS = 600000;

    private final SecureRandom random =
            new SecureRandom();

    public void save(
            Path path,
            KeyPair keyPair,
            char[] password)
            throws GeneralSecurityException, IOException {

        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];

        random.nextBytes(salt);
        random.nextBytes(iv);

        SecretKey key = deriveKey(password, salt);

        Cipher cipher = Cipher.getInstance(
                "AES/GCM/NoPadding");

        cipher.init(
                Cipher.ENCRYPT_MODE,
                key,
                new GCMParameterSpec(TAG_LENGTH, iv)
        );

        byte[] privateBytes =
                keyPair.getPrivate().getEncoded();

        byte[] encrypted;

        try {
            encrypted = cipher.doFinal(privateBytes);
        } finally {
            Arrays.fill(privateBytes, (byte) 0);
        }

        ByteBuffer buffer = ByteBuffer.allocate(
                salt.length + iv.length + encrypted.length);

        buffer.put(salt);
        buffer.put(iv);
        buffer.put(encrypted);

        Files.createDirectories(
                path.toAbsolutePath().getParent());

        Files.write(
                path,
                buffer.array(),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
    }

    public KeyPair load(
            Path path,
            char[] password)
            throws GeneralSecurityException, IOException {

        byte[] data = Files.readAllBytes(path);

        if (data.length <= SALT_LENGTH + IV_LENGTH + 16) {
            throw new GeneralSecurityException(
                    "Invalid key file");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);

        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];

        buffer.get(salt);
        buffer.get(iv);

        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);

        SecretKey key = deriveKey(password, salt);

        Cipher cipher = Cipher.getInstance(
                "AES/GCM/NoPadding");

        cipher.init(
                Cipher.DECRYPT_MODE,
                key,
                new GCMParameterSpec(TAG_LENGTH, iv)
        );

        byte[] privateBytes = cipher.doFinal(encrypted);

        PrivateKey privateKey;

        try {
            privateKey = KeyUtils.decodePrivateKey(privateBytes);
        } finally {
            Arrays.fill(privateBytes, (byte) 0);
        }

        if (!(privateKey instanceof RSAPrivateCrtKey rsa)) {
            throw new GeneralSecurityException(
                    "Invalid RSA private key");
        }

        PublicKey publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(
                        rsa.getModulus(),
                        rsa.getPublicExponent()
                ));

        return new KeyPair(publicKey, privateKey);
    }

    private SecretKey deriveKey(
            char[] password,
            byte[] salt)
            throws GeneralSecurityException {

        if (password == null || password.length < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters");
        }

        PBEKeySpec spec = new PBEKeySpec(
                password,
                salt,
                ITERATIONS,
                256
        );

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256");

            byte[] bytes = factory
                    .generateSecret(spec)
                    .getEncoded();

            try {
                return new SecretKeySpec(bytes, "AES");
            } finally {
                Arrays.fill(bytes, (byte) 0);
            }

        } finally {
            spec.clearPassword();
        }
    }
}