package com.library.management.controller;

import com.library.management.model.Role;
import com.library.management.model.User;
import com.library.management.repository.BookRepository;
import com.library.management.repository.CategoryRepository;
import com.library.management.repository.TransactionRepository;
import com.library.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import java.time.LocalDate;

/**
 * Controller for Admin dashboard and operations.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Admin dashboard with statistics.
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal User currentUser, Model model) {
        // Add current user to model
        model.addAttribute("user", currentUser);

        // Statistics
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalBooks", bookRepository.count());
        model.addAttribute("totalCategories", categoryRepository.count());
        model.addAttribute("availableBooks", bookRepository.findAvailableBooks().size());
        model.addAttribute("activeTransactions", transactionRepository.countByStatus(
                com.library.management.model.TransactionStatus.ISSUED));
        model.addAttribute("overdueBooks", transactionRepository.findOverdueTransactions(LocalDate.now()).size());

        // Recent activities
        model.addAttribute("recentTransactions", 
                transactionRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 5)));

        return "admin/dashboard";
    }

    /**
     * User management page with search and pagination.
     */
    @GetMapping("/users")
    public String userManagement(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<User> usersPage;

        if (!search.isEmpty()) {
            usersPage = userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    search, search, search, pageable);
        } else {
            usersPage = userRepository.findAll(pageable);
        }

        long totalUsers = userRepository.count();
        long studentCount = userRepository.countByRole(Role.STUDENT);
        long adminCount = userRepository.countByRole(Role.ADMIN);
        long activeUsers = userRepository.countByIsActive(true);

        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("totalPages", usersPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("studentCount", studentCount);
        model.addAttribute("adminCount", adminCount);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("search", search);
        model.addAttribute("role", role);
        model.addAttribute("status", status);

        return "admin/users";
    }

    /**
     * View individual user details.
     */
    @GetMapping("/users/{id}")
    public String viewUser(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return userRepository.findById(id)
                .map(user -> {
                    // Get user statistics
                    long borrowedBooks = transactionRepository.countByUserAndStatus(user, 
                            com.library.management.model.TransactionStatus.ISSUED);
                    long totalBorrowed = transactionRepository.countByUser(user);
                    long pendingFines = transactionRepository.countOverdueByUser(user, LocalDate.now());
                    
                    model.addAttribute("user", user);
                    model.addAttribute("borrowedBooks", borrowedBooks);
                    model.addAttribute("totalBorrowed", totalBorrowed);
                    model.addAttribute("pendingFines", pendingFines);
                    
                    // Get recent transactions
                    model.addAttribute("recentTransactions", 
                            transactionRepository.findByUserOrderByIssueDateDesc(user, 
                                    org.springframework.data.domain.PageRequest.of(0, 10)));
                    
                    return "admin/users/view";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "User not found");
                    return "redirect:/admin/users";
                });
    }

    /**
     * Show form to create a new user.
     */
    @GetMapping("/users/new")
    public String showCreateUserForm(Model model) {
        User user = new User();
        user.setIsActive(true); // Default to active
        model.addAttribute("user", user);
        model.addAttribute("isEdit", false);
        model.addAttribute("roles", Role.values());
        return "admin/users/form";
    }

    /**
     * Process new user creation.
     */
    @PostMapping("/users/new")
    public String createUser(
            @Valid @ModelAttribute("user") User user,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("roles", Role.values());
            return "admin/users/form";
        }

        // Check if username already exists
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            model.addAttribute("error", "Username already exists");
            model.addAttribute("isEdit", false);
            model.addAttribute("roles", Role.values());
            return "admin/users/form";
        }

        // Check if email already exists
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            model.addAttribute("error", "Email already exists");
            model.addAttribute("isEdit", false);
            model.addAttribute("roles", Role.values());
            return "admin/users/form";
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", 
                "User '" + user.getUsername() + "' has been created successfully!");
        return "redirect:/admin/users";
    }

    /**
     * Show form to edit an existing user.
     */
    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return userRepository.findById(id)
                .map(user -> {
                    model.addAttribute("user", user);
                    model.addAttribute("isEdit", true);
                    model.addAttribute("roles", Role.values());
                    return "admin/users/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "User not found");
                    return "redirect:/admin/users";
                });
    }

    /**
     * Process user update.
     */
    @PostMapping("/users/edit/{id}")
    public String updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute("user") User user,
            @RequestParam(required = false) String newPassword,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("roles", Role.values());
            return "admin/users/form";
        }

        return userRepository.findById(id)
                .map(existingUser -> {
                    // Update fields
                    existingUser.setUsername(user.getUsername());
                    existingUser.setEmail(user.getEmail());
                    existingUser.setFullName(user.getFullName());
                    existingUser.setPhone(user.getPhone());
                    existingUser.setRole(user.getRole());
                    existingUser.setIsActive(user.getIsActive());

                    // Update password if provided
                    if (newPassword != null && !newPassword.isEmpty()) {
                        existingUser.setPassword(passwordEncoder.encode(newPassword));
                    }

                    userRepository.save(existingUser);
                    redirectAttributes.addFlashAttribute("success", 
                            "User '" + existingUser.getUsername() + "' has been updated successfully!");
                    return "redirect:/admin/users";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "User not found");
                    return "redirect:/admin/users";
                });
    }

    /**
     * Delete a user.
     */
    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return userRepository.findById(id)
                .map(user -> {
                    // Prevent deleting the last admin
                    if (user.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
                        redirectAttributes.addFlashAttribute("error", 
                                "Cannot delete the last admin user!");
                        return "redirect:/admin/users";
                    }

                    userRepository.delete(user);
                    redirectAttributes.addFlashAttribute("success", 
                            "User '" + user.getUsername() + "' has been deleted successfully!");
                    return "redirect:/admin/users";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "User not found");
                    return "redirect:/admin/users";
                });
    }
}
