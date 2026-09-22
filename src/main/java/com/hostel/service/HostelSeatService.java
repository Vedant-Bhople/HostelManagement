package com.hostel.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hostel.model.HostelSeat;
import com.hostel.repository.HostelSeatRepository;

@Service
public class HostelSeatService {

    @Autowired
    private HostelSeatRepository hostelSeatRepository;


    // =====================================================
    // GENERATE ALL SEATS
    // =====================================================

    public String generateSeats() {

        // Prevent duplicate generation

        if (hostelSeatRepository.count() > 0) {

            return "Seats are already generated.";
        }


        String[] branches = {

                "COMPUTER",
                "MECHANICAL",
                "CIVIL",
                "ELECTRICAL",
                "IT"

        };


        String[] years = {

                "1",
                "2",
                "3"

        };


        List<HostelSeat> seats =
                new ArrayList<HostelSeat>();


        // =================================================
        // BOYS RESERVATION
        // =================================================

        /*
         * 11 seats per branch/year
         *
         * SC   = 1
         * ST   = 1
         * OBC  = 2
         * OPEN = 7
         *
         * Total = 11
         */

        String[] boysCategories = {

                "SC",

                "ST",

                "OBC",
                "OBC",

                "OPEN",
                "OPEN",
                "OPEN",
                "OPEN",
                "OPEN",
                "OPEN",
                "OPEN"

        };


        // =================================================
        // BOYS HOSTEL
        // 5 × 3 × 11 = 165
        // =================================================

        for (String branch : branches) {

            for (String year : years) {

                for (int i = 0;
                     i < boysCategories.length;
                     i++) {

                    HostelSeat seat =
                            new HostelSeat();


                    seat.setHostelType("BOYS");

                    seat.setBranch(branch);

                    seat.setYear(year);


                    seat.setSeatNumber(

                            "B-"
                            + branch
                            + "-Y"
                            + year
                            + "-"
                            + String.format(
                                    "%02d",
                                    i + 1
                            )
                    );


                    seat.setReservedCategory(
                            boysCategories[i]
                    );


                    seat.setStatus(
                            "AVAILABLE"
                    );


                    seat.setStudent(null);


                    seats.add(seat);
                }
            }
        }


        // =================================================
        // GIRLS HOSTEL
        // =================================================

        /*
         * Girls have only 3 seats
         * per branch/year.
         *
         * Reservation distribution is kept
         * separately because 1+1+2+7 = 11
         * cannot be applied to a 3-seat pool.
         *
         * For now these are marked OPEN.
         *
         * We will configure the Girls reservation
         * roster separately.
         */

        String[] girlsCategories = {

                "OPEN",
                "OPEN",
                "OPEN"

        };


        // =================================================
        // GIRLS HOSTEL
        // 5 × 3 × 3 = 45
        // =================================================

        for (String branch : branches) {

            for (String year : years) {

                for (int i = 0;
                     i < girlsCategories.length;
                     i++) {

                    HostelSeat seat =
                            new HostelSeat();


                    seat.setHostelType("GIRLS");

                    seat.setBranch(branch);

                    seat.setYear(year);


                    seat.setSeatNumber(

                            "G-"
                            + branch
                            + "-Y"
                            + year
                            + "-"
                            + String.format(
                                    "%02d",
                                    i + 1
                            )
                    );


                    seat.setReservedCategory(
                            girlsCategories[i]
                    );


                    seat.setStatus(
                            "AVAILABLE"
                    );


                    seat.setStudent(null);


                    seats.add(seat);
                }
            }
        }


        // =================================================
        // SAVE ALL
        // =================================================

        hostelSeatRepository.saveAll(seats);


        return seats.size()
                + " hostel seats generated successfully.";
    }


    // =====================================================
    // GET ALL SEATS
    // =====================================================

    public List<HostelSeat> getAllSeats() {

        return hostelSeatRepository.findAll();
    }


    // =====================================================
    // GET BOYS SEATS
    // =====================================================

    public List<HostelSeat> getBoysSeats() {

        return hostelSeatRepository
                .findByHostelType("BOYS");
    }


    // =====================================================
    // GET GIRLS SEATS
    // =====================================================

    public List<HostelSeat> getGirlsSeats() {

        return hostelSeatRepository
                .findByHostelType("GIRLS");
    }


    // =====================================================
    // GET AVAILABLE SEATS
    // =====================================================

    public List<HostelSeat> getAvailableSeats(

            String hostelType,

            String branch,

            String year) {


        return hostelSeatRepository
                .findByHostelTypeAndBranchAndYearAndStatus(

                        hostelType,
                        branch,
                        year,
                        "AVAILABLE"
                );
    }
}