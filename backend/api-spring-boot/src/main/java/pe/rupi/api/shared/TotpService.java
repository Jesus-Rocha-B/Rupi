package pe.rupi.api.shared;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * HU-60: Verificador TOTP RFC 6238 (HMAC-SHA1, 6 dígitos cada 30 segundos) para cuentas docentes.
 * Solo valida secretos almacenados por Django en identidad_doble_factor (Single Writer).
 */
@Service
public class TotpService {
    private static final int PERIOD_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int WINDOW_STEPS = 1;
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final Map<String, Long> lastUsedStep = new ConcurrentHashMap<>();

    public boolean verify(String userId, String secretBase32, String code) {
        return verifyAt(userId, secretBase32, code, Instant.now().getEpochSecond());
    }

    public boolean verifyAt(String userId, String secretBase32, String code, long epochSeconds) {
        String normalized = normalizeCode(code);
        if (normalized == null || secretBase32 == null || secretBase32.isBlank()) {
            return false;
        }
        byte[] secretBytes;
        try {
            secretBytes = decodeBase32(secretBase32);
        } catch (IllegalArgumentException invalidSecret) {
            return false;
        }

        long currentStep = Math.floorDiv(epochSeconds, PERIOD_SECONDS);
        Long previousStep = userId != null ? lastUsedStep.get(userId) : null;
        for (int delta = -WINDOW_STEPS; delta <= WINDOW_STEPS; delta++) {
            long candidateStep = currentStep + delta;
            if (candidateStep < 0) {
                continue;
            }
            String expected = generateCode(secretBytes, candidateStep);
            if (MessageDigest.isEqual(
                    normalized.getBytes(StandardCharsets.US_ASCII),
                    expected.getBytes(StandardCharsets.US_ASCII))) {
                if (previousStep != null && candidateStep <= previousStep) {
                    return false;
                }
                if (userId != null) {
                    if (lastUsedStep.size() > 20_000) {
                        lastUsedStep.clear();
                    }
                    lastUsedStep.put(userId, candidateStep);
                }
                return true;
            }
        }
        return false;
    }

    public static String generateCode(String secretBase32, long epochSeconds) {
        return generateCode(decodeBase32(secretBase32), Math.floorDiv(epochSeconds, PERIOD_SECONDS));
    }

    private static String generateCode(byte[] key, long step) {
        try {
            byte[] counter = ByteBuffer.allocate(8).putLong(step).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(counter);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format("%0" + DIGITS + "d", otp);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el código TOTP.", e);
        }
    }

    private static String normalizeCode(String rawCode) {
        if (rawCode == null) {
            return null;
        }
        String digitsOnly = rawCode.trim().replaceAll("\\s+", "");
        if (digitsOnly.length() != DIGITS) {
            return null;
        }
        for (int i = 0; i < digitsOnly.length(); i++) {
            if (!Character.isDigit(digitsOnly.charAt(i))) {
                return null;
            }
        }
        return digitsOnly;
    }

    static byte[] decodeBase32(String input) {
        String clean = input.trim().replace("=", "").replace(" ", "").toUpperCase();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Secreto Base32 vacío.");
        }
        int buffer = 0;
        int bitsLeft = 0;
        byte[] out = new byte[(clean.length() * 5) / 8];
        int index = 0;
        for (int i = 0; i < clean.length(); i++) {
            int val = BASE32_ALPHABET.indexOf(clean.charAt(i));
            if (val < 0) {
                throw new IllegalArgumentException("Carácter Base32 inválido.");
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                out[index++] = (byte) ((buffer >> bitsLeft) & 0xFF);
            }
        }
        return out;
    }
}
