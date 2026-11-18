package kz.securemessenger.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;

public class RsaKeyService {

    private static final OAEPParameterSpec OAEP_PARAMS =
            new OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT
            );

    
    public KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        return generator.generateKeyPair();
    }

    
    public byte[] wrapKey(SecretKey aesKey, PublicKey publicKey)
            throws GeneralSecurityException {

        Cipher cipher = Cipher.getInstance(
                "RSA/ECB/OAEPWithSHA-256AndMGF1Padding");

        cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_PARAMS);

        return cipher.doFinal(aesKey.getEncoded());
    }

   
    public SecretKey unwrapKey(byte[] encryptedKey, PrivateKey privateKey)
            throws GeneralSecurityException {

        Cipher cipher = Cipher.getInstance(
                "RSA/ECB/OAEPWithSHA-256AndMGF1Padding");

        cipher.init(Cipher.DECRYPT_MODE, privateKey, OAEP_PARAMS);

        byte[] keyBytes = cipher.doFinal(encryptedKey);

        return new SecretKeySpec(keyBytes, "AES");
    }
}
