package com.sentinel;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class HmacValidator {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Calculates hex-encoded HMAC-SHA256 signature for a payload.
     */
    public static String calculateHmac(String payload, String secretKey) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(keySpec);
        byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hmacBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Validates payload against expected signature using constant-time comparison
     * to protect against timing side-channel attacks.
     */
    public static boolean isValidSignature(String payload, String secretKey, String providedSignature) {
        try {
            String computedSignature = calculateHmac(payload, secretKey);
            return MessageDigest.isEqual(
                    computedSignature.getBytes(StandardCharsets.UTF_8),
                    providedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            System.err.println("[SECURITY ALERT] HMAC calculation failed: " + e.getMessage());
            return false;
        }
    }
}
