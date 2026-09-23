package com.hostel.service.reservation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Institutional Reservation Policy Engine.
 * Manages gender-wise hostel seat quotas, capacities, and eligibility rules.
 */
public class ReservationPolicy {

    private static final List<ReservationQuota> BOYS_QUOTAS = new ArrayList<>();
    private static final List<ReservationQuota> GIRLS_QUOTAS = new ArrayList<>();

    static {
        // =====================================================
        // BOYS HOSTEL - 11 SEATS TOTAL
        // =====================================================
        // OPEN / SEBC : 6 seats (Open competition for all eligible students by merit)
        BOYS_QUOTAS.add(new ReservationQuota(
                "OPEN/SEBC",
                6,
                false,
                "OP",
                CategoryNormalizer.OPEN,
                CategoryNormalizer.SEBC
        ));

        // OBC / SBC : 2 seats
        BOYS_QUOTAS.add(new ReservationQuota(
                "OBC/SBC",
                2,
                true,
                "OBC",
                CategoryNormalizer.OBC,
                CategoryNormalizer.SBC
        ));

        // SC / ST : 2 seats
        BOYS_QUOTAS.add(new ReservationQuota(
                "SC/ST",
                2,
                true,
                "SCST",
                CategoryNormalizer.SC,
                CategoryNormalizer.ST
        ));

        // VJ / NT : 1 seat
        BOYS_QUOTAS.add(new ReservationQuota(
                "VJ/NT",
                1,
                true,
                "NT",
                CategoryNormalizer.VJNT,
                CategoryNormalizer.NT
        ));

        // =====================================================
        // GIRLS HOSTEL - 3 SEATS TOTAL
        // =====================================================
        // OPEN / SEBC : 1 seat (Open competition for all eligible students by merit)
        GIRLS_QUOTAS.add(new ReservationQuota(
                "OPEN/SEBC",
                1,
                false,
                "OP",
                CategoryNormalizer.OPEN,
                CategoryNormalizer.SEBC
        ));

        // OBC / SBC : 1 seat
        GIRLS_QUOTAS.add(new ReservationQuota(
                "OBC/SBC",
                1,
                true,
                "OBC",
                CategoryNormalizer.OBC,
                CategoryNormalizer.SBC
        ));

        // SC / ST / VJ / NT : 1 seat
        GIRLS_QUOTAS.add(new ReservationQuota(
                "SC/ST/VJ/NT",
                1,
                true,
                "RES",
                CategoryNormalizer.SC,
                CategoryNormalizer.ST,
                CategoryNormalizer.VJNT,
                CategoryNormalizer.NT
        ));
    }

    /**
     * Gets the ordered list of quota rules for the given gender.
     */
    public static List<ReservationQuota> getQuotasForGender(String gender) {
        if ("BOYS".equalsIgnoreCase(gender)) {
            return Collections.unmodifiableList(BOYS_QUOTAS);
        } else if ("GIRLS".equalsIgnoreCase(gender)) {
            return Collections.unmodifiableList(GIRLS_QUOTAS);
        } else {
            throw new IllegalArgumentException("Invalid gender. Expected BOYS or GIRLS.");
        }
    }

    /**
     * Calculates the total seat capacity for the given gender.
     */
    public static int getTotalCapacity(String gender) {
        return getQuotasForGender(gender).stream()
                .mapToInt(ReservationQuota::getSeatCapacity)
                .sum();
    }

    /**
     * Calculates the total reserved seat capacity for the given gender.
     */
    public static int getReservedCapacity(String gender) {
        return getQuotasForGender(gender).stream()
                .filter(ReservationQuota::isReserved)
                .mapToInt(ReservationQuota::getSeatCapacity)
                .sum();
    }

    /**
     * Calculates the total open seat capacity for the given gender per branch/year group.
     */
    public static int getOpenCapacity(String gender) {
        return getQuotasForGender(gender).stream()
                .filter(q -> !q.isReserved())
                .mapToInt(ReservationQuota::getSeatCapacity)
                .sum();
    }

    public static final List<String> ALL_BRANCHES = Collections.unmodifiableList(
            java.util.Arrays.asList("COMPUTER", "MECHANICAL", "CIVIL", "ELECTRICAL", "IT")
    );

    public static final List<String> ALL_YEARS = Collections.unmodifiableList(
            java.util.Arrays.asList("1", "2", "3")
    );

    /**
     * Calculates the total institutional hostel capacity across all branches and years.
     * Boys: 5 branches * 3 years * 11 seats = 165 total beds
     * Girls: 5 branches * 3 years * 3 seats = 45 total beds
     */
    public static int getTotalInstitutionalCapacity(String gender) {
        int capacityPerGroup = getTotalCapacity(gender);
        return capacityPerGroup * ALL_BRANCHES.size() * ALL_YEARS.size();
    }
}
