package com.hostel.service.reservation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Normalizes student admission categories into standard common categories
 * used for institutional reservation quota matching.
 * 
 * Allowed official categories: OPEN, OBC, SC, ST, VJNT, NT, SEBC.
 * Note: The student's original category is always preserved separately.
 */
public class CategoryNormalizer {

    public static final String OPEN = "OPEN";
    public static final String OBC = "OBC";
    public static final String SBC = "SBC"; // Legacy support only
    public static final String SC = "SC";
    public static final String ST = "ST";
    public static final String VJNT = "VJNT";
    public static final String NT = "NT";
    public static final String SEBC = "SEBC";

    public static final Set<String> ALLOWED_CATEGORIES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(OPEN, OBC, SC, ST, VJNT, NT, SEBC))
    );

    /**
     * Checks if a category string is one of the 7 authorized categories.
     */
    public static boolean isValidCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return false;
        }
        return ALLOWED_CATEGORIES.contains(category.trim().toUpperCase());
    }

    /**
     * Maps an original category string to its normalized common category for quota matching.
     *
     * Rules:
     * - OPEN -> OPEN
     * - OBC -> OBC
     * - SBC -> OBC (Mapped to OBC/SBC reservation quota)
     * - SC -> SC
     * - ST -> ST
     * - VJNT -> VJNT / NT
     * - NT -> NT
     * - VJ, DT, DT/VJ, VJ-A, NT-A, NT-B, NT-C, NT-D -> NT
     * - SEBC -> SEBC
     */
    public static String normalize(String originalCategory) {
        if (originalCategory == null || originalCategory.trim().isEmpty()) {
            return OPEN;
        }

        String raw = originalCategory.trim().toUpperCase()
                .replaceAll("\\s+", " ")
                .replace("_", "-");

        // Direct matches
        if (raw.equals("OPEN") || raw.equals("GENERAL") || raw.equals("GEN")) {
            return OPEN;
        }

        if (raw.equals("OBC")) {
            return OBC;
        }

        if (raw.equals("SBC")) {
            return SBC;
        }

        if (raw.equals("SC")) {
            return SC;
        }

        if (raw.equals("ST")) {
            return ST;
        }

        if (raw.equals("SEBC")) {
            return SEBC;
        }

        if (raw.equals("VJNT") || raw.equals("VJ-NT")) {
            return VJNT;
        }

        if (raw.equals("NT")) {
            return NT;
        }

        // NT/VJ Group legacy variants (VJ, DT, NT-A, NT-B, NT-C, NT-D, NT-1, NT-2, NT-3)
        if (raw.equals("VJ") ||
            raw.equals("DT") ||
            raw.startsWith("DT/VJ") ||
            raw.startsWith("VJ/") ||
            raw.equals("VJ-A") ||
            raw.equals("VJA") ||
            raw.equals("DT-A") ||
            raw.equals("DTA") ||
            raw.startsWith("NT-1") ||
            raw.startsWith("NT1") ||
            raw.startsWith("NT-2") ||
            raw.startsWith("NT2") ||
            raw.startsWith("NT-3") ||
            raw.startsWith("NT3") ||
            raw.startsWith("NT-A") ||
            raw.startsWith("NTA") ||
            raw.startsWith("NT-B") ||
            raw.startsWith("NTB") ||
            raw.startsWith("NT-C") ||
            raw.startsWith("NTC") ||
            raw.startsWith("NT-D") ||
            raw.startsWith("NTD")) {
            return NT;
        }

        return OPEN;
    }
}
