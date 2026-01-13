package com.library.management.controller;

import com.library.management.model.*;
import com.library.management.repository.*;
import com.library.management.service.ReportService;
import com.library.management.service.ScheduledTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller for admin reports and analytics.
 */
@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ScheduledTaskService scheduledTaskService;
    private final TransactionRepository transactionRepository;
    private final FineRepository fineRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    /**
     * Reports dashboard.
     */
    @GetMapping
    public String reportsDashboard(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("dashboardStats", reportService.getDashboardStatistics());
        model.addAttribute("popularBooks", reportService.getMostPopularBooks(5));

        return "admin/reports/dashboard";
    }

    /**
     * Book inventory report.
     */
    @GetMapping("/inventory")
    public String inventoryReport(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("report", reportService.getBookInventoryReport());

        return "admin/reports/inventory";
    }

    /**
     * Overdue books report.
     */
    @GetMapping("/overdue")
    public String overdueReport(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("report", reportService.getOverdueReport());

        return "admin/reports/overdue";
    }

    /**
     * Transaction report.
     */
    @GetMapping("/transactions")
    public String transactionReport(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();

        model.addAttribute("user", user);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("status", status);
        
        // Get paginated transactions
        model.addAttribute("transactions", transactionRepository.findAllByOrderByIssueDateDesc(PageRequest.of(page, 20)));
        
        // Stats
        model.addAttribute("totalTransactions", transactionRepository.count());
        model.addAttribute("issuedCount", transactionRepository.countByStatus(TransactionStatus.ISSUED));
        model.addAttribute("returnedCount", transactionRepository.countByStatus(TransactionStatus.RETURNED));
        model.addAttribute("overdueCount", transactionRepository.countOverdueTransactions(LocalDate.now()));

        return "admin/reports/transactions";
    }

    /**
     * Fine report.
     */
    @GetMapping("/fines")
    public String fineReport(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();

        model.addAttribute("user", user);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("status", status);
        
        // Get paginated fines
        model.addAttribute("fines", fineRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, 20)));
        
        // Stats
        model.addAttribute("totalFines", fineRepository.count());
        model.addAttribute("pendingAmount", fineRepository.getTotalPendingFines() != null ? fineRepository.getTotalPendingFines() : 0.0);
        model.addAttribute("collectedAmount", fineRepository.getTotalPaidFines() != null ? fineRepository.getTotalPaidFines() : 0.0);
        model.addAttribute("waivedAmount", 0.0);

        return "admin/reports/fines";
    }

    /**
     * User activity report.
     */
    @GetMapping("/users")
    public String userActivityReport(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);
        
        // Get all users
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        
        // Stats
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("activeUsers", userRepository.findByIsActiveTrue().size());
        model.addAttribute("newUsersThisMonth", 0L); // Simplified
        model.addAttribute("usersWithFines", userRepository.findUsersWithPendingFines().size());
        model.addAttribute("mostActiveUsers", users.stream().limit(10).toList());

        return "admin/reports/users";
    }

    /**
     * Reservation report.
     */
    @GetMapping("/reservations")
    public String reservationReport(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        
        model.addAttribute("user", user);
        model.addAttribute("status", status);
        
        // Get paginated reservations
        model.addAttribute("reservations", reservationRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, 20)));
        
        // Stats
        model.addAttribute("totalReservations", reservationRepository.count());
        model.addAttribute("pendingCount", reservationRepository.countByStatus(ReservationStatus.PENDING));
        model.addAttribute("readyCount", reservationRepository.countByStatus(ReservationStatus.READY));
        model.addAttribute("fulfilledCount", reservationRepository.countByStatus(ReservationStatus.FULFILLED));
        model.addAttribute("cancelledCount", reservationRepository.countByStatus(ReservationStatus.CANCELLED));
        model.addAttribute("expiredCount", reservationRepository.countByStatus(ReservationStatus.EXPIRED));
        
        return "admin/reports/reservations";
    }

    // ==================== Manual Task Triggers ====================

    /**
     * Trigger fine calculation manually.
     */
    @PostMapping("/trigger/fines")
    public String triggerFineCalculation(RedirectAttributes redirectAttributes) {
        int count = scheduledTaskService.triggerFineCalculation();
        redirectAttributes.addFlashAttribute("successMessage", 
            "Fine calculation triggered. Total pending fines: " + count);
        return "redirect:/admin/reports";
    }

    /**
     * Trigger reservation processing manually.
     */
    @PostMapping("/trigger/reservations")
    public String triggerReservationProcessing(RedirectAttributes redirectAttributes) {
        long count = scheduledTaskService.triggerReservationProcessing();
        redirectAttributes.addFlashAttribute("successMessage", 
            "Reservation processing triggered. Ready reservations: " + count);
        return "redirect:/admin/reports";
    }

    // ==================== Export Endpoints ====================

    /**
     * Export overdue report as CSV.
     */
    @GetMapping("/export/overdue")
    public ResponseEntity<byte[]> exportOverdueReport() {
        Map<String, Object> report = reportService.getOverdueReport();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) report.get("overdueItems");

        String csv = reportService.generateCSV(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=overdue_report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }
}
