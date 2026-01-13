package com.library.management.service;

import com.library.management.model.*;
import com.library.management.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for StudentService.
 * 
 * SCD CONCEPT: UNIT TESTING
 * Tests student-related operations including borrowing,
 * returning, reservations, and fine management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudentService Unit Tests")
class StudentServiceTest {
    
    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private ReservationRepository reservationRepository;
    
    @Mock
    private FineRepository fineRepository;
    
    @Mock
    private NotificationRepository notificationRepository;
    
    @Mock
    private CategoryRepository categoryRepository;
    
    @InjectMocks
    private StudentService studentService;
    
    private User testUser;
    private Book testBook;
    private Transaction testTransaction;
    private Reservation testReservation;
    
    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("student1");
        testUser.setEmail("student1@test.com");
        testUser.setRole(Role.STUDENT);
        testUser.setIsActive(true);
        
        // Setup test category
        Category category = new Category();
        category.setId(1L);
        category.setName("Fiction");
        
        // Setup test book
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setQuantity(5);
        testBook.setAvailableQuantity(3);
        testBook.setCategory(category);
        testBook.setIsActive(true);
        
        // Setup test transaction
        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setUser(testUser);
        testTransaction.setBook(testBook);
        testTransaction.setIssueDate(LocalDate.now().minusDays(7));
        testTransaction.setDueDate(LocalDate.now().plusDays(7));
        testTransaction.setStatus(TransactionStatus.ISSUED);
        
        // Setup test reservation
        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setUser(testUser);
        testReservation.setBook(testBook);
        testReservation.setReservationDate(LocalDate.now());
        testReservation.setStatus(ReservationStatus.PENDING);
        testReservation.setQueuePosition(1);
    }
    
    // ==================== Book Borrowing Tests ====================
    
    @Test
    @DisplayName("Should get current borrowed books for user")
    void getCurrentBorrowedBooks_Success() {
        // Arrange
        List<Transaction> transactions = Arrays.asList(testTransaction);
        when(transactionRepository.findByUserAndStatus(testUser, TransactionStatus.ISSUED))
                .thenReturn(transactions);
        
        // Act
        List<Transaction> result = studentService.getCurrentBorrowedBooks(testUser);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Book", result.get(0).getBook().getTitle());
    }
    
    @Test
    @DisplayName("Should return empty list when no borrowed books")
    void getCurrentBorrowedBooks_Empty() {
        // Arrange
        when(transactionRepository.findByUserAndStatus(testUser, TransactionStatus.ISSUED))
                .thenReturn(Collections.emptyList());
        
        // Act
        List<Transaction> result = studentService.getCurrentBorrowedBooks(testUser);
        
        // Assert
        assertTrue(result.isEmpty());
    }
    
    @Test
    @DisplayName("Should get returned books history")
    void getReturnedBooks_Success() {
        // Arrange
        testTransaction.setStatus(TransactionStatus.RETURNED);
        testTransaction.setReturnDate(LocalDate.now());
        
        when(transactionRepository.findByUserAndStatus(testUser, TransactionStatus.RETURNED))
                .thenReturn(Arrays.asList(testTransaction));
        
        // Act
        List<Transaction> result = studentService.getReturnedBooks(testUser);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    // ==================== Reservation Tests ====================
    
    @Test
    @DisplayName("Should reserve book successfully")
    void reserveBook_Success() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(transactionRepository.existsByUserAndBookAndStatusIn(any(), any(), any()))
                .thenReturn(false);
        when(reservationRepository.existsByUserAndBookAndStatus(any(), any(), any()))
                .thenReturn(false);
        when(reservationRepository.findByBookAndStatusOrderByQueuePositionAsc(any(), any()))
                .thenReturn(Collections.emptyList());
        when(reservationRepository.save(any(Reservation.class)))
                .thenReturn(testReservation);
        
        // Act
        Reservation result = studentService.reserveBook(testUser, 1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(ReservationStatus.PENDING, result.getStatus());
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }
    
    @Test
    @DisplayName("Should fail reservation when book not found")
    void reserveBook_BookNotFound() {
        // Arrange
        when(bookRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            studentService.reserveBook(testUser, 999L);
        });
    }
    
    @Test
    @DisplayName("Should fail when user already has book borrowed")
    void reserveBook_AlreadyBorrowed() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(transactionRepository.existsByUserAndBookAndStatusIn(any(), any(), any()))
                .thenReturn(true);
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            studentService.reserveBook(testUser, 1L);
        });
    }
    
    @Test
    @DisplayName("Should get user pending reservations")
    void getUserPendingReservations() {
        // Arrange
        when(reservationRepository.findByUserAndStatus(testUser, ReservationStatus.PENDING))
                .thenReturn(Arrays.asList(testReservation));
        
        // Act
        List<Reservation> result = studentService.getUserPendingReservations(testUser);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    @DisplayName("Should cancel reservation successfully")
    void cancelReservation_Success() {
        // Arrange
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        
        // Act
        studentService.cancelReservation(testUser, 1L);
        
        // Assert
        verify(reservationRepository, times(1)).save(any(Reservation.class));
        assertEquals(ReservationStatus.CANCELLED, testReservation.getStatus());
    }
    
    // ==================== Fine Tests ====================
    
    @Test
    @DisplayName("Should get user fines")
    void getUserFines() {
        // Arrange
        Fine fine = new Fine();
        fine.setId(1L);
        fine.setUser(testUser);
        fine.setAmount(5.0);
        fine.setStatus(FineStatus.PENDING);
        
        when(fineRepository.findByUser(testUser)).thenReturn(Arrays.asList(fine));
        
        // Act
        List<Fine> result = studentService.getUserFines(testUser);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5.0, result.get(0).getAmount());
    }
    
    @Test
    @DisplayName("Should get pending fines only")
    void getPendingFines() {
        // Arrange
        Fine pendingFine = new Fine();
        pendingFine.setId(1L);
        pendingFine.setUser(testUser);
        pendingFine.setAmount(10.0);
        pendingFine.setStatus(FineStatus.PENDING);
        
        when(fineRepository.findByUserAndStatus(testUser, FineStatus.PENDING))
                .thenReturn(Arrays.asList(pendingFine));
        
        // Act
        List<Fine> result = studentService.getPendingFines(testUser);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(FineStatus.PENDING, result.get(0).getStatus());
    }
    
    // ==================== Statistics Tests ====================
    
    @Test
    @DisplayName("Should get user statistics")
    void getUserStatistics() {
        // Arrange
        when(transactionRepository.countByUserAndStatus(eq(testUser), eq(TransactionStatus.ISSUED)))
                .thenReturn(1L);
        when(transactionRepository.findByUser(testUser))
                .thenReturn(Arrays.asList(testTransaction));
        when(transactionRepository.findByUserAndStatus(eq(testUser), eq(TransactionStatus.ISSUED)))
                .thenReturn(Arrays.asList(testTransaction));
        when(reservationRepository.findByUserAndStatus(eq(testUser), eq(ReservationStatus.PENDING)))
                .thenReturn(Collections.emptyList());
        when(fineRepository.getTotalPendingFinesByUser(testUser))
                .thenReturn(0.0);
        
        // Act
        Map<String, Object> stats = studentService.getUserStatistics(testUser);
        
        // Assert
        assertNotNull(stats);
        assertTrue(stats.containsKey("currentBorrowed"));
        assertTrue(stats.containsKey("totalBorrowed"));
    }
    
    // ==================== Category Tests ====================
    
    @Test
    @DisplayName("Should get all categories")
    void getAllCategories() {
        // Arrange
        Category category = new Category();
        category.setId(1L);
        category.setName("Fiction");
        
        when(categoryRepository.findByIsActiveTrueOrderByNameAsc())
                .thenReturn(Arrays.asList(category));
        
        // Act
        List<Category> result = studentService.getAllCategories();
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Fiction", result.get(0).getName());
    }
}
