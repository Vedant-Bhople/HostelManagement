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
        // 11 seats per branch/year (Total 165)
        // OPEN  = 6 (Merit based)
        // OBC   = 2
        // SC/ST = 2 (SC 1, ST 1)
        // NT    = 1
        // Total = 11
        // =================================================

        String[] boysCategories = {
                "OPEN",
                "OPEN",
                "OPEN",
                "OPEN",
                "OPEN",
                "OPEN",
                "OBC",
                "OBC",
                "SC",
                "ST",
                "NT"
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
        // GIRLS RESERVATION
        // 3 seats per branch/year (Total 45)
        // OPEN = 1 (Merit based)
        // OBC  = 1
        // Other Reserved = 1
        // Total = 3
        // =================================================

        String[] girlsCategories = {
                "OPEN",
                "OBC",
                "RESERVED"
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