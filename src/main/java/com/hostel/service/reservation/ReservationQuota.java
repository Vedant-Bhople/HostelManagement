package com.hostel.service.reservation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Defines a reservation quota with its seat capacity, eligible categories,
 * and reservation classification.
 */
public class ReservationQuota {

    private final String name;
    private final int seatCapacity;
    private final boolean reserved;
    private final String seatCategoryCode;
    private final Set<String> eligibleCommonCategories;

    public ReservationQuota(
            String name,
            int seatCapacity,
            boolean reserved,
            String seatCategoryCode,
            String... eligibleCommonCategories) {
        this.name = name;
        this.seatCapacity = seatCapacity;
        this.reserved = reserved;
        this.seatCategoryCode = seatCategoryCode;
        this.eligibleCommonCategories = Collections.unmodifiableSet(
                new HashSet<>(Arrays.asList(eligibleCommonCategories))
        );
    }

    public String getName() {
        return name;
    }

    public int getSeatCapacity() {
        return seatCapacity;
    }

    public boolean isReserved() {
        return reserved;
    }

    public String getSeatCategoryCode() {
        return seatCategoryCode;
    }

    public Set<String> getEligibleCommonCategories() {
        return eligibleCommonCategories;
    }

    /**
     * Checks if a student with the given common category is eligible for this quota.
     * For OPEN / non-reserved quotas, all students are eligible purely on merit.
     */
    public boolean isEligible(String commonCategory) {
        if (!this.reserved) {
            return true; // OPEN quota is open to all applicants purely on merit
        }
        if (commonCategory == null) {
            return false;
        }
        return eligibleCommonCategories.contains(commonCategory.trim().toUpperCase());
    }
}
