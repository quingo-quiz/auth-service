package tech.arhr.quingo.auth_service.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class EncryptionUtil {

    @Value("${spring.security.encryption.key}")
    private String encryptionKey;

    private static final String ALGORITHM = "AES";
    private SecretKeySpec secretKey;

    @PostConstruct
    public void init() {
        if (encryptionKey == null || encryptionKey.length() < 16) {
            throw new IllegalArgumentException("Encryption key must be at least 16 characters long!");
        }
        this.secretKey = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
    }

    public String encrypt(String rawData) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(rawData.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error while encrypting data", e);
        }
    }

    public String decrypt(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error while decrypting data", e);
        }
    }
}