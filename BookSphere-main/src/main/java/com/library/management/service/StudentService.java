package com.library.management.service;

import com.library.management.dto.BookResponse;
import com.library.management.dto.ProfileUpdateRequest;
import com.library.management.dto.PasswordChangeRequest;
import com.library.management.model.*;
import com.library.management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class for Student operations.
 * Handles book browsing, reservations, borrowed books, fines, and profile management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StudentService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ==================== Book Search & Browse ====================

    /**
     * Search books with filters.
     */
    @Transactional(readOnly = true)
    public Page<BookResponse> searchBooks(String search, Long categoryId, String availability, 
                                           int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Book> books;

        if (categoryId != null && categoryId > 0) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category != null) {
                if (search != null && !search.trim().isEmpty()) {
                    books = bookRepository.searchBooksByCategory(search.trim(), category, pageable);
                } else {
                    books = bookRepository.findByCategoryAndIsActiveTrue(category, pageable);
                }
            } else {
                books = Page.empty();
            }
        } else if (search != null && !search.trim().isEmpty()) {
            books = bookRepository.searchBooks(search.trim(), pageable);
        } else if ("available".equalsIgnoreCase(availability)) {
            books = bookRepository.findAvailableBooks(pageable);
        } else {
            books = bookRepository.findByIsActiveTrue(pageable);
        }

        return books.map(BookResponse::fromEntity);
    }

    /**
     * Get book details by ID.
     */
    @Transactional(readOnly = true)
    public BookResponse getBookDetails(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));
        return BookResponse.fromEntity(book);
    }

    /**
     * Check if a book is available for borrowing.
     */
    @Transactional(readOnly = true)
    public boolean isBookAvailable(Long bookId) {
        return bookRepository.findById(bookId)
                .map(book -> book.getAvailableQuantity() > 0 && book.getIsActive())
                .orElse(false);
    }

    /**
     * Get all active categories.
     */
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findByIsActiveTrueOrderByNameAsc();
    }

    // ==================== Book Reservation ====================

    /**
     * Reserve a book for a user.
     */
    public Reservation reserveBook(User user, Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));

        // Check if user already has this book borrowed
        if (transactionRepository.existsByUserAndBookAndStatusIn(user, book, 
                List.of(TransactionStatus.ISSUED))) {
            throw new IllegalStateException("You already have this book borrowed");
        }

        // Check if user already has a pending reservation for this book
        if (reservationRepository.existsByUserAndBookAndStatus(user, book, ReservationStatus.PENDING)) {
            throw new IllegalStateException("You already have a pending reservation for this book");
        }

        // Calculate queue position
        List<Reservation> existingReservations = reservationRepository
                .findByBookAndStatusOrderByQueuePositionAsc(book, ReservationStatus.PENDING);
        int queuePosition = existingReservations.size() + 1;

        Reservation reservation = Reservation.builder()
                .user(user)
                .book(book)
                .reservationDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusDays(7)) // Reservation expires in 7 days
                .status(ReservationStatus.PENDING)
                .queuePosition(queuePosition)
                .build();

        Reservation saved = reservationRepository.save(reservation);
        log.info("User {} reserved book '{}' at queue position {}", 
                user.getUsername(), book.getTitle(), queuePosition);

        return saved;
    }

    /**
     * Cancel a reservation.
     */
    public void cancelReservation(User user, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("You can only cancel your own reservations");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        // Update queue positions for remaining reservations
        updateQueuePositions(reservation.getBook());

        log.info("User {} cancelled reservation for book '{}'", 
                user.getUsername(), reservation.getBook().getTitle());
    }

    /**
     * Get user's reservations.
     */
    @Transactional(readOnly = true)
    public List<Reservation> getUserReservations(User user) {
        return reservationRepository.findByUser(user);
    }

    /**
     * Get user's pending reservations.
     */
    @Transactional(readOnly = true)
    public List<Reservation> getUserPendingReservations(User user) {
        return reservationRepository.findByUserAndStatus(user, ReservationStatus.PENDING);
    }

    /**
     * Update queue positions after a cancellation.
     */
    private void updateQueuePositions(Book book) {
        List<Reservation> pendingReservations = reservationRepository
                .findByBookAndStatusOrderByQueuePositionAsc(book, ReservationStatus.PENDING);
        
        int position = 1;
        for (Reservation res : pendingReservations) {
            res.setQueuePosition(position++);
            reservationRepository.save(res);
        }
    }

    // ==================== My Books (Borrowed Books) ====================

    /**
     * Get user's currently borrowed books.
     */
    @Transactional(readOnly = true)
    public List<Transaction> getCurrentBorrowedBooks(User user) {
        return transactionRepository.findByUserAndStatus(user, TransactionStatus.ISSUED);
    }

    /**
     * Get user's borrowing history.
     */
    @Transactional(readOnly = true)
    public Page<Transaction> getBorrowingHistory(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "issueDate"));
        return transactionRepository.findByUser(user, pageable);
    }

    /**
     * Get user's returned books history.
     */
    @Transactional(readOnly = true)
    public List<Transaction> getReturnedBooks(User user) {
        return transactionRepository.findByUserAndStatus(user, TransactionStatus.RETURNED);
    }

    /**
     * Check if a book is due soon (within 3 days).
     */
    @Transactional(readOnly = true)
    public boolean isBookDueSoon(Transaction transaction) {
        if (transaction.getDueDate() == null) return false;
        return transaction.getDueDate().isBefore(LocalDate.now().plusDays(3)) &&
               !transaction.getDueDate().isBefore(LocalDate.now());
    }

    /**
     * Check if a book is overdue.
     */
    @Transactional(readOnly = true)
    public boolean isBookOverdue(Transaction transaction) {
        if (transaction.getDueDate() == null) return false;
        return transaction.getDueDate().isBefore(LocalDate.now());
    }

    // ==================== Fine Management ====================

    /**
     * Get user's fines.
     */
    @Transactional(readOnly = true)
    public List<Fine> getUserFines(User user) {
        return fineRepository.findByUser(user);
    }

    /**
     * Get user's pending fines.
     */
    @Transactional(readOnly = true)
    public List<Fine> getPendingFines(User user) {
        return fineRepository.findByUserAndStatus(user, FineStatus.PENDING);
    }

    /**
     * Get total pending fine amount for user.
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalPendingFines(User user) {
        Double total = fineRepository.getTotalPendingFinesByUser(user);
        return total != null ? BigDecimal.valueOf(total) : BigDecimal.ZERO;
    }

    // ==================== Profile Management ====================

    /**
     * Update user profile.
     */
    public User updateProfile(User user, ProfileUpdateRequest request) {
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());

        User savedUser = userRepository.save(user);
        log.info("User {} updated their profile", user.getUsername());

        return savedUser;
    }

    /**
     * Change user password.
     */
    public void changePassword(User user, PasswordChangeRequest request) {
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Verify new passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }

        // Validate new password strength
        if (request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("User {} changed their password", user.getUsername());
    }

    /**
     * Get user's borrowing statistics.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStatistics(User user) {
        Map<String, Object> stats = new HashMap<>();

        // Current borrowed count
        stats.put("currentBorrowed", transactionRepository.countByUserAndStatus(user, TransactionStatus.ISSUED));

        // Total books borrowed (all time)
        stats.put("totalBorrowed", transactionRepository.findByUser(user).size());

        // Pending reservations
        stats.put("pendingReservations", reservationRepository.findByUserAndStatus(user, ReservationStatus.PENDING).size());

        // Total fines pending
        stats.put("pendingFines", getTotalPendingFines(user));

        // Overdue books
        List<Transaction> currentBooks = getCurrentBorrowedBooks(user);
        long overdueCount = currentBooks.stream()
                .filter(this::isBookOverdue)
                .count();
        stats.put("overdueBooks", overdueCount);

        return stats;
    }

    /**
     * Check if user has reached borrowing limit.
     */
    @Transactional(readOnly = true)
    public boolean hasReachedBorrowingLimit(User user) {
        long currentBorrowed = transactionRepository.countByUserAndStatus(user, TransactionStatus.ISSUED);
        return currentBorrowed >= 5; // Max 5 books at a time
    }

    /**
     * Check if user can reserve a book.
     */
    @Transactional(readOnly = true)
    public boolean canReserveBook(User user, Long bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null || !book.getIsActive()) {
            return false;
        }

        // Check if already borrowed
        if (transactionRepository.existsByUserAndBookAndStatusIn(user, book, 
                List.of(TransactionStatus.ISSUED))) {
            return false;
        }

        // Check if already reserved
        if (reservationRepository.existsByUserAndBookAndStatus(user, book, ReservationStatus.PENDING)) {
            return false;
        }

        // Check pending reservations limit (max 3)
        long pendingReservations = reservationRepository.findByUserAndStatus(user, ReservationStatus.PENDING).size();
        return pendingReservations < 3;
    }
}
