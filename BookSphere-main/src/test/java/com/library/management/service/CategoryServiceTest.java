package com.library.management.service;

import com.library.management.dto.CategoryRequest;
import com.library.management.dto.CategoryResponse;
import com.library.management.model.Category;
import com.library.management.repository.CategoryRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for CategoryService.
 * 
 * Phase 8: Testing & Quality Assurance
 * Tests CRUD operations and business logic for categories.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {
    
    @Mock
    private CategoryRepository categoryRepository;
    
    @InjectMocks
    private CategoryService categoryService;
    
    private Category testCategory;
    private CategoryRequest testCategoryRequest;
    
    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Fiction");
        testCategory.setDescription("Fiction books");
        testCategory.setIsActive(true);
        
        testCategoryRequest = new CategoryRequest();
        testCategoryRequest.setName("Science Fiction");
        testCategoryRequest.setDescription("Sci-Fi books");
        testCategoryRequest.setIsActive(true);
    }
    
    // ==================== Create Tests ====================
    
    @Test
    @DisplayName("Should create category successfully")
    void createCategory_Success() {
        // Arrange
        when(categoryRepository.existsByName(testCategoryRequest.getName())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category cat = invocation.getArgument(0);
            cat.setId(2L);
            return cat;
        });
        
        // Act
        CategoryResponse response = categoryService.createCategory(testCategoryRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals("Science Fiction", response.getName());
        verify(categoryRepository).save(any(Category.class));
    }
    
    @Test
    @DisplayName("Should throw exception when category name exists")
    void createCategory_DuplicateName_ThrowsException() {
        // Arrange
        when(categoryRepository.existsByName(testCategoryRequest.getName())).thenReturn(true);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.createCategory(testCategoryRequest);
        });
        verify(categoryRepository, never()).save(any());
    }
    
    // ==================== Read Tests ====================
    
    @Test
    @DisplayName("Should get category by ID")
    void getCategoryById_Success() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        
        // Act
        Optional<CategoryResponse> response = categoryService.getCategoryById(1L);
        
        // Assert
        assertTrue(response.isPresent());
        assertEquals("Fiction", response.get().getName());
    }
    
    @Test
    @DisplayName("Should return empty when category not found")
    void getCategoryById_NotFound() {
        // Arrange
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Act
        Optional<CategoryResponse> response = categoryService.getCategoryById(99L);
        
        // Assert
        assertTrue(response.isEmpty());
    }
    
    @Test
    @DisplayName("Should get all active categories")
    void getAllActiveCategories_Success() {
        // Arrange
        List<Category> categories = Arrays.asList(testCategory);
        when(categoryRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(categories);
        
        // Act
        List<CategoryResponse> responses = categoryService.getAllActiveCategories();
        
        // Assert
        assertEquals(1, responses.size());
        assertEquals("Fiction", responses.get(0).getName());
    }
    
    @Test
    @DisplayName("Should get categories with pagination")
    void getCategoriesWithPagination_Success() {
        // Arrange
        Page<Category> categoryPage = new PageImpl<>(Arrays.asList(testCategory));
        when(categoryRepository.findByIsActiveTrue(any(Pageable.class))).thenReturn(categoryPage);
        
        // Act
        Page<CategoryResponse> response = categoryService.getAllCategories(PageRequest.of(0, 10));
        
        // Assert
        assertEquals(1, response.getTotalElements());
    }
    
    // ==================== Update Tests ====================
    
    @Test
    @DisplayName("Should update category successfully")
    void updateCategory_Success() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);
        
        // Act
        CategoryResponse response = categoryService.updateCategory(1L, testCategoryRequest);
        
        // Assert
        assertNotNull(response);
        verify(categoryRepository).save(any(Category.class));
    }
    
    @Test
    @DisplayName("Should throw exception when updating non-existent category")
    void updateCategory_NotFound_ThrowsException() {
        // Arrange
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.updateCategory(99L, testCategoryRequest);
        });
    }
    
    // ==================== Delete Tests ====================
    
    @Test
    @DisplayName("Should soft delete category")
    void deleteCategory_Success() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);
        
        // Act
        categoryService.deleteCategory(1L);
        
        // Assert
        verify(categoryRepository).save(argThat(cat -> !cat.getIsActive()));
    }
    
    @Test
    @DisplayName("Should throw exception when deleting non-existent category")
    void deleteCategory_NotFound_ThrowsException() {
        // Arrange
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.deleteCategory(99L);
        });
    }
    
    // ==================== Search Tests ====================
    
    @Test
    @DisplayName("Should search categories by term")
    void searchCategories_Success() {
        // Arrange
        Page<Category> categoryPage = new PageImpl<>(Arrays.asList(testCategory));
        when(categoryRepository.searchCategories(eq("Fic"), any(Pageable.class))).thenReturn(categoryPage);
        
        // Act
        Page<CategoryResponse> responses = categoryService.searchCategories("Fic", PageRequest.of(0, 10));
        
        // Assert
        assertEquals(1, responses.getTotalElements());
    }
    
    // ==================== Count Tests ====================
    
    @Test
    @DisplayName("Should count active categories")
    void countActiveCategories_Success() {
        // Arrange
        when(categoryRepository.countByIsActiveTrue()).thenReturn(5L);
        
        // Act
        long count = categoryService.countActiveCategories();
        
        // Assert
        assertEquals(5L, count);
    }
}
