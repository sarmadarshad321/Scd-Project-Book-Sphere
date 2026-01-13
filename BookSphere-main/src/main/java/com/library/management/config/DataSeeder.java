package com.library.management.config;

import com.library.management.model.*;
import com.library.management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Data Seeder Configuration.
 * Populates the database with initial sample data for development and testing.
 * Only runs in 'dev' profile or when database is empty.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Command line runner to seed initial data.
     * Runs when the application starts.
     */
    @Bean
    @Profile("!test") // Don't run during tests
    public CommandLineRunner seedData() {
        return args -> {
            // Only seed if database is empty
            if (userRepository.count() == 0) {
                log.info("Seeding initial data...");
                seedUsers();
                seedCategories();
                seedBooks();
                log.info("Data seeding completed successfully!");
            } else {
                log.info("Database already contains data. Skipping seeding.");
            }
        };
    }

    /**
     * Seed default users (admin and sample students).
     */
    private void seedUsers() {
        log.info("Creating default users...");

        // Create Admin User
        User admin = User.builder()
                .username("admin")
                .email("admin@library.com")
                .password(passwordEncoder.encode("admin123"))
                .fullName("System Administrator")
                .phone("123-456-7890")
                .address("Library Main Office")
                .role(Role.ADMIN)
                .isActive(true)
                .build();
        userRepository.save(admin);
        log.info("Admin user created: admin / admin123");

        // Create Sample Students
        List<User> students = Arrays.asList(
                User.builder()
                        .username("student1")
                        .email("student1@university.edu")
                        .password(passwordEncoder.encode("student123"))
                        .fullName("John Doe")
                        .phone("111-222-3333")
                        .address("123 University Ave")
                        .role(Role.STUDENT)
                        .isActive(true)
                        .build(),
                User.builder()
                        .username("student2")
                        .email("student2@university.edu")
                        .password(passwordEncoder.encode("student123"))
                        .fullName("Jane Smith")
                        .phone("444-555-6666")
                        .address("456 College Street")
                        .role(Role.STUDENT)
                        .isActive(true)
                        .build(),
                User.builder()
                        .username("student3")
                        .email("student3@university.edu")
                        .password(passwordEncoder.encode("student123"))
                        .fullName("Bob Johnson")
                        .phone("777-888-9999")
                        .address("789 Campus Drive")
                        .role(Role.STUDENT)
                        .isActive(true)
                        .build()
        );
        userRepository.saveAll(students);
        log.info("Sample students created: student1, student2, student3 / student123");
    }

    /**
     * Seed book categories.
     */
    private void seedCategories() {
        log.info("Creating book categories...");

        List<Category> categories = Arrays.asList(
                Category.builder()
                        .name("Fiction")
                        .description("Fictional literature including novels, short stories, and narrative works")
                        .isActive(true)
                        .build(),
                Category.builder()
                        .name("Non-Fiction")
                        .description("Factual literature including biographies, essays, and documentaries")
                        .isActive(true)
                        .build(),
                Category.builder()
                        .name("Science & Technology")
                        .description("Books on scientific research, technology, and engineering")
                        .isActive(true)
                        .build(),
                Category.builder()
                        .name("Computer Science")
                        .description("Programming, software development, algorithms, and computer theory")
                        .isActive(true)
                        .build(),
                Category.builder()
                        .name("Mathematics")
                        .description("Mathematical concepts, theories, and applications")
                        .isActive(true)
                        .build(),
                Category.builder()
                        .name("History")
                        .description("Historical accounts, events, and civilizations")
                        .isActive(true)
                        .build(),
                Category.builder()
                        .name("Philosophy")
                        .description("Philosophical works and theories")
                        .isActive(true)
                        .build(),
                Category.builder()
                        .name("Business & Economics")
                        .description("Business management, economics, and finance")
                        .isActive(true)
                        .build(),
                Category.builder()
                        .name("Self-Help")
                        .description("Personal development and self-improvement books")
                        .isActive(true)
                        .build(),
                Category.builder()
                        .name("Reference")
                        .description("Encyclopedias, dictionaries, and reference materials")
                        .isActive(true)
                        .build()
        );
        categoryRepository.saveAll(categories);
        log.info("Created {} categories", categories.size());
    }

    /**
     * Seed sample books.
     */
    private void seedBooks() {
        log.info("Creating sample books...");

        Category fiction = categoryRepository.findByName("Fiction").orElse(null);
        Category computerScience = categoryRepository.findByName("Computer Science").orElse(null);
        Category science = categoryRepository.findByName("Science & Technology").orElse(null);
        Category business = categoryRepository.findByName("Business & Economics").orElse(null);
        Category selfHelp = categoryRepository.findByName("Self-Help").orElse(null);

        List<Book> books = Arrays.asList(
                // Fiction Books
                Book.builder()
                        .title("To Kill a Mockingbird")
                        .author("Harper Lee")
                        .isbn("9780061120084")
                        .publisher("Harper Perennial")
                        .publicationYear(1960)
                        .quantity(5)
                        .availableQuantity(5)
                        .description("A classic novel about racial injustice in the American South")
                        .shelfLocation("A-101")
                        .category(fiction)
                        .isActive(true)
                        .build(),
                Book.builder()
                        .title("1984")
                        .author("George Orwell")
                        .isbn("9780451524935")
                        .publisher("Signet Classics")
                        .publicationYear(1949)
                        .quantity(4)
                        .availableQuantity(4)
                        .description("A dystopian social science fiction novel")
                        .shelfLocation("A-102")
                        .category(fiction)
                        .isActive(true)
                        .build(),
                Book.builder()
                        .title("The Great Gatsby")
                        .author("F. Scott Fitzgerald")
                        .isbn("9780743273565")
                        .publisher("Scribner")
                        .publicationYear(1925)
                        .quantity(3)
                        .availableQuantity(3)
                        .description("A novel about the American Dream in the Jazz Age")
                        .shelfLocation("A-103")
                        .category(fiction)
                        .isActive(true)
                        .build(),

                // Computer Science Books
                Book.builder()
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .isbn("9780132350884")
                        .publisher("Prentice Hall")
                        .publicationYear(2008)
                        .quantity(6)
                        .availableQuantity(6)
                        .description("A handbook of agile software craftsmanship")
                        .shelfLocation("B-201")
                        .category(computerScience)
                        .isActive(true)
                        .build(),
                Book.builder()
                        .title("Design Patterns")
                        .author("Gang of Four")
                        .isbn("9780201633610")
                        .publisher("Addison-Wesley")
                        .publicationYear(1994)
                        .quantity(4)
                        .availableQuantity(4)
                        .description("Elements of Reusable Object-Oriented Software")
                        .shelfLocation("B-202")
                        .category(computerScience)
                        .isActive(true)
                        .build(),
                Book.builder()
                        .title("Introduction to Algorithms")
                        .author("Thomas H. Cormen")
                        .isbn("9780262033848")
                        .publisher("MIT Press")
                        .publicationYear(2009)
                        .quantity(5)
                        .availableQuantity(5)
                        .description("Comprehensive textbook on algorithms")
                        .shelfLocation("B-203")
                        .category(computerScience)
                        .isActive(true)
                        .build(),
                Book.builder()
                        .title("The Pragmatic Programmer")
                        .author("David Thomas, Andrew Hunt")
                        .isbn("9780135957059")
                        .publisher("Addison-Wesley")
                        .publicationYear(2019)
                        .quantity(4)
                        .availableQuantity(4)
                        .description("Your Journey to Mastery, 20th Anniversary Edition")
                        .shelfLocation("B-204")
                        .category(computerScience)
                        .isActive(true)
                        .build(),

                // Science Books
                Book.builder()
                        .title("A Brief History of Time")
                        .author("Stephen Hawking")
                        .isbn("9780553380163")
                        .publisher("Bantam")
                        .publicationYear(1988)
                        .quantity(3)
                        .availableQuantity(3)
                        .description("From the Big Bang to Black Holes")
                        .shelfLocation("C-301")
                        .category(science)
                        .isActive(true)
                        .build(),
                Book.builder()
                        .title("The Selfish Gene")
                        .author("Richard Dawkins")
                        .isbn("9780198788607")
                        .publisher("Oxford University Press")
                        .publicationYear(1976)
                        .quantity(3)
                        .availableQuantity(3)
                        .description("A revolutionary view of evolution")
                        .shelfLocation("C-302")
                        .category(science)
                        .isActive(true)
                        .build(),

                // Business Books
                Book.builder()
                        .title("The Lean Startup")
                        .author("Eric Ries")
                        .isbn("9780307887894")
                        .publisher("Crown Business")
                        .publicationYear(2011)
                        .quantity(4)
                        .availableQuantity(4)
                        .description("How Today's Entrepreneurs Use Continuous Innovation")
                        .shelfLocation("D-401")
                        .category(business)
                        .isActive(true)
                        .build(),
                Book.builder()
                        .title("Zero to One")
                        .author("Peter Thiel")
                        .isbn("9780804139298")
                        .publisher("Crown Business")
                        .publicationYear(2014)
                        .quantity(3)
                        .availableQuantity(3)
                        .description("Notes on Startups, or How to Build the Future")
                        .shelfLocation("D-402")
                        .category(business)
                        .isActive(true)
                        .build(),

                // Self-Help Books
                Book.builder()
                        .title("Atomic Habits")
                        .author("James Clear")
                        .isbn("9780735211292")
                        .publisher("Avery")
                        .publicationYear(2018)
                        .quantity(5)
                        .availableQuantity(5)
                        .description("An Easy & Proven Way to Build Good Habits & Break Bad Ones")
                        .shelfLocation("E-501")
                        .category(selfHelp)
                        .isActive(true)
                        .build(),
                Book.builder()
                        .title("Think and Grow Rich")
                        .author("Napoleon Hill")
                        .isbn("9781585424337")
                        .publisher("TarcherPerigee")
                        .publicationYear(1937)
                        .quantity(4)
                        .availableQuantity(4)
                        .description("The landmark bestseller about achieving success")
                        .shelfLocation("E-502")
                        .category(selfHelp)
                        .isActive(true)
                        .build()
        );

        bookRepository.saveAll(books);
        log.info("Created {} sample books", books.size());
    }
}
