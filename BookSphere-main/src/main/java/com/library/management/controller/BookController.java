package com.library.management.controller;

import com.library.management.dto.BookRequest;
import com.library.management.dto.BookResponse;
import com.library.management.dto.CategoryResponse;
import com.library.management.service.BookService;
import com.library.management.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for Admin Book Management operations.
 */
@Controller
@RequestMapping("/admin/books")
@RequiredArgsConstructor
@Slf4j
public class BookController {

    private final BookService bookService;
    private final CategoryService categoryService;

    /**
     * Display list of all books with search and pagination.
     */
    @GetMapping
    public String listBooks(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Model model) {

        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<BookResponse> books;
        if (categoryId != null) {
            books = bookService.searchBooksByCategory(search, categoryId, pageable);
        } else if (!search.isEmpty()) {
            books = bookService.searchBooks(search, pageable);
        } else {
            books = bookService.getAllBooks(pageable);
        }

        List<CategoryResponse> categories = categoryService.getAllActiveCategoriesLight();

        model.addAttribute("books", books);
        model.addAttribute("categories", categories);
        model.addAttribute("search", search);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        return "admin/books/list";
    }

    /**
     * Display book details.
     */
    @GetMapping("/{id}")
    public String viewBook(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return bookService.getBookById(id)
                .map(book -> {
                    model.addAttribute("book", book);
                    return "admin/books/view";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Book not found");
                    return "redirect:/admin/books";
                });
    }

    /**
     * Display form to add a new book.
     */
    @GetMapping({"/add", "/new"})
    public String showAddForm(Model model) {
        model.addAttribute("bookRequest", new BookRequest());
        model.addAttribute("categories", categoryService.getAllActiveCategoriesLight());
        model.addAttribute("isEdit", false);
        return "admin/books/form";
    }

    /**
     * Process new book creation.
     */
    @PostMapping("/add")
    public String addBook(
            @Valid @ModelAttribute("bookRequest") BookRequest bookRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllActiveCategoriesLight());
            model.addAttribute("isEdit", false);
            return "admin/books/form";
        }

        try {
            BookResponse savedBook = bookService.createBook(bookRequest);
            redirectAttributes.addFlashAttribute("success", 
                    "Book '" + savedBook.getTitle() + "' has been added successfully!");
            return "redirect:/admin/books";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.getAllActiveCategoriesLight());
            model.addAttribute("isEdit", false);
            return "admin/books/form";
        }
    }

    /**
     * Display form to edit an existing book.
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return bookService.getBookById(id)
                .map(book -> {
                    BookRequest bookRequest = BookRequest.builder()
                            .title(book.getTitle())
                            .author(book.getAuthor())
                            .isbn(book.getIsbn())
                            .publisher(book.getPublisher())
                            .publicationYear(book.getPublicationYear())
                            .quantity(book.getQuantity())
                            .description(book.getDescription())
                            .coverImage(book.getCoverImage())
                            .shelfLocation(book.getShelfLocation())
                            .categoryId(book.getCategoryId())
                            .isActive(book.getIsActive())
                            .build();
                    
                    model.addAttribute("bookRequest", bookRequest);
                    model.addAttribute("bookId", id);
                    model.addAttribute("categories", categoryService.getAllActiveCategoriesLight());
                    model.addAttribute("isEdit", true);
                    return "admin/books/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Book not found");
                    return "redirect:/admin/books";
                });
    }

    /**
     * Process book update.
     */
    @PostMapping("/edit/{id}")
    public String updateBook(
            @PathVariable Long id,
            @Valid @ModelAttribute("bookRequest") BookRequest bookRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("bookId", id);
            model.addAttribute("categories", categoryService.getAllActiveCategoriesLight());
            model.addAttribute("isEdit", true);
            return "admin/books/form";
        }

        try {
            BookResponse updatedBook = bookService.updateBook(id, bookRequest);
            redirectAttributes.addFlashAttribute("success", 
                    "Book '" + updatedBook.getTitle() + "' has been updated successfully!");
            return "redirect:/admin/books";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("bookId", id);
            model.addAttribute("categories", categoryService.getAllActiveCategoriesLight());
            model.addAttribute("isEdit", true);
            return "admin/books/form";
        }
    }

    /**
     * Delete a book (soft delete).
     */
    @PostMapping("/delete/{id}")
    public String deleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookService.deleteBook(id);
            redirectAttributes.addFlashAttribute("success", "Book has been deleted successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/books";
    }

    /**
     * Restore a soft-deleted book.
     */
    @PostMapping("/restore/{id}")
    public String restoreBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookService.restoreBook(id);
            redirectAttributes.addFlashAttribute("success", "Book has been restored successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/books";
    }
}
