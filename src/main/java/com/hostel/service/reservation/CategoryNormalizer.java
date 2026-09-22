package com.hostel.service.reservation;

/**
 * Normalizes student admission categories into standard common categories
 * used for institutional reservation quota matching.
 * 
 * Note: The student's original category is always preserved separately.
 */
public class CategoryNormalizer {

    public static final String OPEN = "OPEN";
    public static final String OBC = "OBC";
    public static final String SBC = "SBC";
    public static final String SC = "SC";
    public static final String ST = "ST";
    public static final String NT = "NT";
    public static final String SEBC = "SEBC";

    /**
     * Maps an original category string to its normalized common category.
     *
     * Rules:
     * - OPEN -> OPEN
     * - OBC -> OBC
     * - SBC -> SBC
     * - SC -> SC
     * - ST -> ST
     * - VJ, DT, DT/VJ, DT/VJ NT-A, VJ-A, VJNT -> NT
     * - NT-1, NT-1 NT-B, NT-B -> NT
     * - NT-2, NT-2 NT-C, NT-C -> NT
     * - NT-3, NT-3 NT-D, NT-D -> NT
     * - NT-A, NT-B, NT-C, NT-D, NT -> NT
     * - SEBC -> SEBC
     * - OTHER, EWS, or unmapped -> OPEN
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

        // NT Group variants (VJ, DT, NT-A, NT-B, NT-C, NT-D, NT-1, NT-2, NT-3)
        if (raw.equals("NT") ||
            raw.equals("VJ") ||
            raw.equals("DT") ||
            raw.equals("VJNT") ||
            raw.equals("VJ-NT") ||
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

        // OTHER / EWS / TFWS / PWD or other categories treat common quota as OPEN
        return OPEN;
    }
}
