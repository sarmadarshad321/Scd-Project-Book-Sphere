package com.library.management.controller;

import com.library.management.dto.BookResponse;
import com.library.management.dto.PasswordChangeRequest;
import com.library.management.dto.ProfileUpdateRequest;
import com.library.management.model.*;
import com.library.management.repository.*;
import com.library.management.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * Controller for Student dashboard and operations.
 * Handles book browsing, reservations, borrowed books, fines, and profile management.
 */
@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
@Slf4j
public class StudentController {

    private final StudentService studentService;
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;

    // ==================== Dashboard ====================

    /**
     * Student dashboard with personal information and statistics.
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User currentUser, Model model) {
        model.addAttribute("user", currentUser);

        // Get statistics
        Map<String, Object> stats = studentService.getUserStatistics(currentUser);
        model.addAttribute("borrowedBooks", stats.get("currentBorrowed"));
        model.addAttribute("pendingReservations", stats.get("pendingReservations"));
        model.addAttribute("pendingFines", stats.get("pendingFines"));
        model.addAttribute("overdueBooks", stats.get("overdueBooks"));

        // Available books count
        model.addAttribute("availableBooks", bookRepository.findAvailableBooks().size());

        // Currently borrowed books
        List<Transaction> currentBooks = studentService.getCurrentBorrowedBooks(currentUser);
        model.addAttribute("currentBooks", currentBooks);

        // Helper for checking due dates
        model.addAttribute("studentService", studentService);

        // Recent activity (last 5 transactions)
        model.addAttribute("recentTransactions",
                transactionRepository.findByUser(currentUser,
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))));

        return "student/dashboard";
    }

    // ==================== Book Search & Browse ====================

    /**
     * Browse and search books.
     */
    @GetMapping("/books")
    public String browseBooks(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "all") String availability,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @AuthenticationPrincipal User currentUser,
            Model model) {

        Page<BookResponse> books = studentService.searchBooks(search, categoryId, availability,
                page, size, sortBy, sortDir);

        model.addAttribute("user", currentUser);
        model.addAttribute("books", books);
        model.addAttribute("categories", studentService.getAllCategories());
        model.addAttribute("search", search);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("availability", availability);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", books.getTotalPages());
        model.addAttribute("totalItems", books.getTotalElements());

        return "student/books";
    }

    /**
     * View book details.
     */
    @GetMapping("/books/{id}")
    public String viewBook(@PathVariable Long id,
                          @AuthenticationPrincipal User currentUser,
                          Model model) {
        BookResponse book = studentService.getBookDetails(id);
        model.addAttribute("book", book);
        model.addAttribute("user", currentUser);

        // Check if user can reserve this book
        model.addAttribute("canReserve", studentService.canReserveBook(currentUser, id));

        // Check if book is available
        model.addAttribute("isAvailable", studentService.isBookAvailable(id));

        // Check if user already has this book borrowed
        Book bookEntity = bookRepository.findById(id).orElse(null);
        boolean hasBorrowed = bookEntity != null && 
                transactionRepository.existsByUserAndBookAndStatusIn(currentUser, bookEntity,
                        List.of(TransactionStatus.ISSUED));
        model.addAttribute("hasBorrowed", hasBorrowed);

        // Check if user has a pending reservation
        boolean hasReservation = bookEntity != null &&
                reservationRepository.existsByUserAndBookAndStatus(currentUser, bookEntity, ReservationStatus.PENDING);
        model.addAttribute("hasReservation", hasReservation);

        return "student/book-detail";
    }

    // ==================== Book Reservation ====================

    /**
     * Reserve a book.
     */
    @PostMapping("/books/{id}/reserve")
    public String reserveBook(@PathVariable Long id,
                             @AuthenticationPrincipal User currentUser,
                             RedirectAttributes redirectAttributes) {
        try {
            studentService.reserveBook(currentUser, id);
            redirectAttributes.addFlashAttribute("successMessage", "Book reserved successfully! You are in the queue.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/student/books/" + id;
    }

    /**
     * View user's reservations.
     */
    @GetMapping("/reservations")
    public String myReservations(@AuthenticationPrincipal User currentUser, Model model) {
        model.addAttribute("user", currentUser);
        model.addAttribute("reservations", studentService.getUserReservations(currentUser));
        model.addAttribute("pendingReservations", studentService.getUserPendingReservations(currentUser));
        return "student/reservations";
    }

    /**
     * Cancel a reservation.
     */
    @PostMapping("/reservations/{id}/cancel")
    public String cancelReservation(@PathVariable Long id,
                                   @AuthenticationPrincipal User currentUser,
                                   RedirectAttributes redirectAttributes) {
        try {
            studentService.cancelReservation(currentUser, id);
            redirectAttributes.addFlashAttribute("successMessage", "Reservation cancelled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/student/reservations";
    }

    // ==================== My Books (Borrowed Books) ====================

    /**
     * View currently borrowed books and history.
     */
    @GetMapping("/my-books")
    public String myBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser,
            Model model) {

        model.addAttribute("user", currentUser);

        // Currently borrowed books
        List<Transaction> currentBooks = studentService.getCurrentBorrowedBooks(currentUser);
        model.addAttribute("currentBooks", currentBooks);

        // Borrowing history with pagination
        Page<Transaction> history = studentService.getBorrowingHistory(currentUser, page, size);
        model.addAttribute("history", history);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", history.getTotalPages());

        // Helper for checking due dates
        model.addAttribute("studentService", studentService);

        // Stats
        model.addAttribute("totalBorrowed", history.getTotalElements());
        model.addAttribute("currentlyBorrowed", currentBooks.size());

        return "student/my-books";
    }

    // ==================== Fine Management ====================

    /**
     * View fines.
     */
    @GetMapping("/fines")
    public String myFines(@AuthenticationPrincipal User currentUser, Model model) {
        model.addAttribute("user", currentUser);
        model.addAttribute("fines", studentService.getUserFines(currentUser));
        model.addAttribute("pendingFines", studentService.getPendingFines(currentUser));
        model.addAttribute("totalPending", studentService.getTotalPendingFines(currentUser));
        return "student/fines";
    }

    // ==================== Profile Management ====================

    /**
     * View profile.
     */
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal User currentUser, Model model) {
        model.addAttribute("user", currentUser);
        model.addAttribute("profileRequest", ProfileUpdateRequest.builder()
                .fullName(currentUser.getFullName())
                .email(currentUser.getEmail())
                .phone(currentUser.getPhone())
                .address(currentUser.getAddress())
                .build());
        model.addAttribute("passwordRequest", new PasswordChangeRequest());
        model.addAttribute("stats", studentService.getUserStatistics(currentUser));
        return "student/profile";
    }

    /**
     * Update profile.
     */
    @PostMapping("/profile/update")
    public String updateProfile(@Valid @ModelAttribute("profileRequest") ProfileUpdateRequest request,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal User currentUser,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", currentUser);
            model.addAttribute("passwordRequest", new PasswordChangeRequest());
            model.addAttribute("stats", studentService.getUserStatistics(currentUser));
            model.addAttribute("activeTab", "profile");
            return "student/profile";
        }

        try {
            studentService.updateProfile(currentUser, request);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/profile";
    }

    /**
     * Change password.
     */
    @PostMapping("/profile/change-password")
    public String changePassword(@Valid @ModelAttribute("passwordRequest") PasswordChangeRequest request,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal User currentUser,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", currentUser);
            model.addAttribute("profileRequest", ProfileUpdateRequest.builder()
                    .fullName(currentUser.getFullName())
                    .email(currentUser.getEmail())
                    .phone(currentUser.getPhone())
                    .address(currentUser.getAddress())
                    .build());
            model.addAttribute("stats", studentService.getUserStatistics(currentUser));
            model.addAttribute("activeTab", "security");
            return "student/profile";
        }

        try {
            studentService.changePassword(currentUser, request);
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/profile";
    }
}
