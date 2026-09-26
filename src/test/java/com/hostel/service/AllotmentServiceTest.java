package com.hostel.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hostel.model.Allotment;
import com.hostel.model.Application;
import com.hostel.model.MeritList;
import com.hostel.repository.AllotmentRepository;
import com.hostel.repository.ApplicationRepository;
import com.hostel.repository.MeritListRepository;

@ExtendWith(MockitoExtension.class)
public class AllotmentServiceTest {

    @Mock
    private MeritListRepository meritListRepository;

    @Mock
    private AllotmentRepository allotmentRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AllotmentService allotmentService;

    private List<MeritList> createMockMeritList(String gender, String branch, String year) {
        // Example from prompt:
        // Rank 1: A (OBC), 2: B (OPEN), 3: C (SC), 4: D (OPEN), 5: E (OBC), 6: F (ST),
        // 7: G (NT), 8: H (OBC), 9: I (SC), 10: J (OPEN), 11: K (OPEN), 12: L (OBC), 13: M (SC)
        String[] names = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M"};
        String[] categories = {"OBC", "OPEN", "SC", "OPEN", "OBC", "ST", "NT", "OBC", "SC", "OPEN", "OPEN", "OBC", "SC"};

        List<MeritList> list = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            MeritList m = new MeritList();
            m.setId((long) (i + 1));
            m.setMeritRank(i + 1);
            m.setStudentName(names[i]);
            m.setGender(gender);
            m.setBranch(branch);
            m.setYear(year);
            m.setCategory(categories[i]);
            m.setAggregate(95.0 - i);
            m.setPublished(true);

            Application app = new Application();
            app.setId((long) (i + 100));
            app.setFullName(names[i]);
            app.setGender(gender);
            app.setBranch(branch);
            app.setYear(year);
            app.setCategory(categories[i]);
            app.setAggregate(95.0 - i);
            m.setApplication(app);

            list.add(m);
        }
        return list;
    }

    @Test
    public void testBoysAllotmentLogicWithMeritBasedOpenAndReserved() {
        List<MeritList> mockList = createMockMeritList("BOYS", "COMPUTER", "3");
        when(meritListRepository.findByGenderAndBranchAndYearOrderByMeritRankAsc("BOYS", "COMPUTER", "3"))
                .thenReturn(mockList);

        when(allotmentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Allotment> result = allotmentService.generateBranchAllotment("BOYS", "COMPUTER", "3");

        assertEquals(13, result.size());

        // 1. Check OPEN allotments (6 seats): Top 6 candidates (Rank 1 to 6) get OPEN regardless of original category
        assertEquals("OPEN", result.get(0).getAllotmentCategory());
        assertEquals("OBC", result.get(0).getCategory()); // Original category preserved!
        assertEquals("ALLOTTED", result.get(0).getAllotmentStatus());
        assertEquals("B-COMP-Y3-OP-01", result.get(0).getSeatNumber());

        assertEquals("OPEN", result.get(1).getAllotmentCategory());
        assertEquals("OPEN", result.get(1).getCategory());
        assertEquals("B-COMP-Y3-OP-02", result.get(1).getSeatNumber());

        assertEquals("OPEN", result.get(2).getAllotmentCategory());
        assertEquals("SC", result.get(2).getCategory());
        assertEquals("B-COMP-Y3-OP-03", result.get(2).getSeatNumber());

        assertEquals("OPEN", result.get(3).getAllotmentCategory());
        assertEquals("OPEN", result.get(3).getCategory());
        assertEquals("B-COMP-Y3-OP-04", result.get(3).getSeatNumber());

        assertEquals("OPEN", result.get(4).getAllotmentCategory());
        assertEquals("OBC", result.get(4).getCategory());
        assertEquals("B-COMP-Y3-OP-05", result.get(4).getSeatNumber());

        assertEquals("OPEN", result.get(5).getAllotmentCategory());
        assertEquals("ST", result.get(5).getCategory());
        assertEquals("B-COMP-Y3-OP-06", result.get(5).getSeatNumber());

        // 2. Check Reserved seat allotments (remaining students):
        // OBC (2 seats): H (Rank 8) and L (Rank 12)
        Allotment hAllot = result.stream().filter(a -> "H".equals(a.getApplication().getFullName())).findFirst().orElse(null);
        assertNotNull(hAllot);
        assertEquals("OBC", hAllot.getAllotmentCategory());
        assertEquals("B-COMP-Y3-OBC-01", hAllot.getSeatNumber());

        Allotment lAllot = result.stream().filter(a -> "L".equals(a.getApplication().getFullName())).findFirst().orElse(null);
        assertNotNull(lAllot);
        assertEquals("OBC", lAllot.getAllotmentCategory());
        assertEquals("B-COMP-Y3-OBC-02", lAllot.getSeatNumber());

        // SC/ST (2 seats): I (Rank 9 - SC) and M (Rank 13 - SC)
        Allotment iAllot = result.stream().filter(a -> "I".equals(a.getApplication().getFullName())).findFirst().orElse(null);
        assertNotNull(iAllot);
        assertEquals("SC/ST", iAllot.getAllotmentCategory());
        assertEquals("B-COMP-Y3-SCST-01", iAllot.getSeatNumber());

        Allotment mAllot = result.stream().filter(a -> "M".equals(a.getApplication().getFullName())).findFirst().orElse(null);
        assertNotNull(mAllot);
        assertEquals("SC/ST", mAllot.getAllotmentCategory());
        assertEquals("B-COMP-Y3-SCST-02", mAllot.getSeatNumber());

        // NT (1 seat): G (Rank 7 - NT)
        Allotment gAllot = result.stream().filter(a -> "G".equals(a.getApplication().getFullName())).findFirst().orElse(null);
        assertNotNull(gAllot);
        assertEquals("NT", gAllot.getAllotmentCategory());
        assertEquals("B-COMP-Y3-NT-01", gAllot.getSeatNumber());

        // 3. Waiting List: Rank 10 (J - OPEN) and Rank 11 (K - OPEN)
        Allotment jAllot = result.stream().filter(a -> "J".equals(a.getApplication().getFullName())).findFirst().orElse(null);
        assertNotNull(jAllot);
        assertEquals("WAITING", jAllot.getAllotmentStatus());
        assertEquals("WAITING-01", jAllot.getSeatNumber());

        Allotment kAllot = result.stream().filter(a -> "K".equals(a.getApplication().getFullName())).findFirst().orElse(null);
        assertNotNull(kAllot);
        assertEquals("WAITING", kAllot.getAllotmentStatus());
        assertEquals("WAITING-02", kAllot.getSeatNumber());
    }

    @Test
    public void testConvertUnusedReservedSeatsToOpen() {
        // Simulate a scenario where 2 reserved seats are vacant:
        // e.g. 6 OPEN filled, 1 OBC filled (1 vacant), 1 SC/ST filled (1 vacant), 1 NT filled -> 2 vacant reserved seats
        // Waiting students: Rank 10 (J) and Rank 11 (K)
        List<MeritList> mockList = createMockMeritList("BOYS", "CIVIL", "2");
        when(meritListRepository.findByGenderAndBranchAndYearOrderByMeritRankAsc("BOYS", "CIVIL", "2"))
                .thenReturn(mockList.subList(0, 10)); // fewer students so reserved seats remain vacant

        when(allotmentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Allotment> initialAllotments = allotmentService.generateBranchAllotment("BOYS", "CIVIL", "2");

        when(allotmentRepository.findByGenderAndBranchAndYearOrderByMeritRankAsc("BOYS", "CIVIL", "2"))
                .thenReturn(initialAllotments);

        // Convert unused reserved seats
        List<Allotment> convertedList = allotmentService.convertUnusedReservedSeats("BOYS", "CIVIL", "2");

        // Check that waiting students received converted OPEN seats
        long convertedCount = convertedList.stream().filter(a -> Boolean.TRUE.equals(a.getIsConverted())).count();
        assertTrue(convertedCount > 0);
    }

    @Test
    public void testGirlsAllotmentLogic() {
        // 3 seats total: 1 OPEN, 1 OBC, 1 Other Reserved
        List<MeritList> mockList = createMockMeritList("GIRLS", "COMPUTER", "1");
        when(meritListRepository.findByGenderAndBranchAndYearOrderByMeritRankAsc("GIRLS", "COMPUTER", "1"))
                .thenReturn(mockList);

        when(allotmentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Allotment> result = allotmentService.generateBranchAllotment("GIRLS", "COMPUTER", "1");

        // 1 OPEN seat goes to Rank 1 (A - OBC)
        assertEquals("OPEN", result.get(0).getAllotmentCategory());
        assertEquals("OBC", result.get(0).getCategory());
        assertEquals("G-COMP-Y1-OP-01", result.get(0).getSeatNumber());

        // 1 OBC seat goes to Rank 5 (E - OBC) (since B is OPEN, C is SC, D is OPEN)
        Allotment eAllot = result.stream().filter(a -> "E".equals(a.getApplication().getFullName())).findFirst().orElse(null);
        assertNotNull(eAllot);
        assertEquals("OBC", eAllot.getAllotmentCategory());
        assertEquals("G-COMP-Y1-OBC-01", eAllot.getSeatNumber());

        // 1 Other Reserved seat goes to Rank 3 (C - SC)
        Allotment cAllot = result.stream().filter(a -> "C".equals(a.getApplication().getFullName())).findFirst().orElse(null);
        assertNotNull(cAllot);
        assertEquals("Other Reserved", cAllot.getAllotmentCategory());
        assertEquals("G-COMP-Y1-RES-01", cAllot.getSeatNumber());

        // Other students (B, D, F, G, H, etc.) should be in WAITING
        Allotment bAllot = result.stream().filter(a -> "B".equals(a.getApplication().getFullName())).findFirst().orElse(null);
        assertNotNull(bAllot);
        assertEquals("WAITING", bAllot.getAllotmentStatus());
    }

    @Test
    public void testCombinedYearAllotmentGeneration() {
        // Generate for "ALL" branches in BOYS Year 3
        for (String branch : ReservationPolicy.ALL_BRANCHES) {
            List<MeritList> mockList = createMockMeritList("BOYS", branch, "3");
            when(meritListRepository.findByGenderAndBranchAndYearOrderByMeritRankAsc("BOYS", branch, "3"))
                    .thenReturn(mockList);
        }

        when(allotmentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Allotment> result = allotmentService.generateAllotment("BOYS", "ALL", "3");

        // 5 branches * 13 students each = 65 total processed
        assertEquals(65, result.size());

        // 5 branches * 11 seats allotted = 55 total allotted seats
        long allottedCount = result.stream()
                .filter(a -> "ALLOTTED".equalsIgnoreCase(a.getAllotmentStatus()))
                .count();
        assertEquals(55, allottedCount);
    }
}
