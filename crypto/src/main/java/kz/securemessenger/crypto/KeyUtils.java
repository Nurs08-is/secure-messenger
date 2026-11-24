package kz.securemessenger.crypto;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.interfaces.RSAKey;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public final class KeyUtils {

    private KeyUtils() {
    }

    public static String encodePublicKey(PublicKey key) {

        return Base64.getEncoder()
                .encodeToString(key.getEncoded());
    }

    public static PublicKey decodePublicKey(String encoded)
            throws GeneralSecurityException {

        byte[] bytes = Base64.getDecoder()
                .decode(encoded);

        KeyFactory factory = KeyFactory.getInstance("RSA");

        PublicKey key = factory.generatePublic(
                new X509EncodedKeySpec(bytes));

        validateRsaKey(key);

        return key;
    }

    public static PrivateKey decodePrivateKey(byte[] bytes)
            throws GeneralSecurityException {

        KeyFactory factory = KeyFactory.getInstance("RSA");

        PrivateKey key = factory.generatePrivate(
                new PKCS8EncodedKeySpec(bytes));

        validateRsaKey(key);

        return key;
    }

    private static void validateRsaKey(java.security.Key key)
            throws GeneralSecurityException {

        if (!(key instanceof RSAKey rsaKey) ||
                rsaKey.getModulus().bitLength() != 2048) {

            throw new GeneralSecurityException(
                    "RSA-2048 key is required");
        }
    }
}