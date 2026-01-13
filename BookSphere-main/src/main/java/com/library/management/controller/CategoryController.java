package com.library.management.controller;

import com.library.management.dto.CategoryRequest;
import com.library.management.dto.CategoryResponse;
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

/**
 * Controller for Admin Category Management operations.
 */
@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Display list of all categories with search and pagination.
     */
    @GetMapping
    public String listCategories(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Model model) {

        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CategoryResponse> categories;
        if (!search.isEmpty()) {
            categories = categoryService.searchCategories(search, pageable);
        } else {
            categories = categoryService.getAllCategories(pageable);
        }

        model.addAttribute("categories", categories);
        model.addAttribute("search", search);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        return "admin/categories/list";
    }

    /**
     * Display category details.
     */
    @GetMapping("/{id}")
    public String viewCategory(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return categoryService.getCategoryById(id)
                .map(category -> {
                    model.addAttribute("category", category);
                    return "admin/categories/view";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Category not found");
                    return "redirect:/admin/categories";
                });
    }

    /**
     * Display form to add a new category.
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("categoryRequest", new CategoryRequest());
        model.addAttribute("isEdit", false);
        return "admin/categories/form";
    }

    /**
     * Process new category creation.
     */
    @PostMapping("/add")
    public String addCategory(
            @Valid @ModelAttribute("categoryRequest") CategoryRequest categoryRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "admin/categories/form";
        }

        try {
            CategoryResponse savedCategory = categoryService.createCategory(categoryRequest);
            redirectAttributes.addFlashAttribute("success", 
                    "Category '" + savedCategory.getName() + "' has been added successfully!");
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("isEdit", false);
            return "admin/categories/form";
        }
    }

    /**
     * Display form to edit an existing category.
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return categoryService.getCategoryById(id)
                .map(category -> {
                    CategoryRequest categoryRequest = CategoryRequest.builder()
                            .name(category.getName())
                            .description(category.getDescription())
                            .isActive(category.getIsActive())
                            .build();
                    
                    model.addAttribute("categoryRequest", categoryRequest);
                    model.addAttribute("categoryId", id);
                    model.addAttribute("isEdit", true);
                    return "admin/categories/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Category not found");
                    return "redirect:/admin/categories";
                });
    }

    /**
     * Process category update.
     */
    @PostMapping("/edit/{id}")
    public String updateCategory(
            @PathVariable Long id,
            @Valid @ModelAttribute("categoryRequest") CategoryRequest categoryRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categoryId", id);
            model.addAttribute("isEdit", true);
            return "admin/categories/form";
        }

        try {
            CategoryResponse updatedCategory = categoryService.updateCategory(id, categoryRequest);
            redirectAttributes.addFlashAttribute("success", 
                    "Category '" + updatedCategory.getName() + "' has been updated successfully!");
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categoryId", id);
            model.addAttribute("isEdit", true);
            return "admin/categories/form";
        }
    }

    /**
     * Delete a category (soft delete).
     */
    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Category has been deleted successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    /**
     * Restore a soft-deleted category.
     */
    @PostMapping("/restore/{id}")
    public String restoreCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.restoreCategory(id);
            redirectAttributes.addFlashAttribute("success", "Category has been restored successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
