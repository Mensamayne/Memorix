package io.memorix.deduplication;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for generating content hashes.
 * 
 * <p>Uses SHA-256 for generating deterministic hashes from text content.
 * Supports content normalization for better duplicate detection.
 */
public class ContentHashGenerator {
    
    private static final MessageDigest MESSAGE_DIGEST = createDigest();
    
    private static MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Generate hash from content.
     * 
     * @param content Content to hash
     * @param normalize Whether to normalize content before hashing
     * @return Hex-encoded hash string
     */
    public static String generateHash(String content, boolean normalize) {
        if (content == null) {
            return null;
        }
        
        String processedContent = normalize ? normalizeContent(content) : content;
        
        synchronized (MESSAGE_DIGEST) {
            MESSAGE_DIGEST.reset();
            byte[] hashBytes = MESSAGE_DIGEST.digest(processedContent.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        }
    }
    
    /**
     * Normalize content for consistent hashing.
     * 
     * <p>Normalization steps:
     * <ul>
     *   <li>Trim leading/trailing whitespace</li>
     *   <li>Convert to lowercase</li>
     *   <li>Collapse multiple spaces to single space</li>
     *   <li>Remove line breaks</li>
     * </ul>
     * 
     * @param content Content to normalize
     * @return Normalized content
     */
    public static String normalizeContent(String content) {
        if (content == null) {
            return null;
        }
        
        return content
            .trim()
            .toLowerCase()
            .replaceAll("\\s+", " ")  // Collapse multiple spaces
            .replaceAll("[\\r\\n]+", " ");  // Remove line breaks
    }
    
    /**
     * Convert byte array to hexadecimal string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

