package com.library.management.security;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security Tests for Authentication and Authorization.
 * 
 * Phase 8: Testing & Quality Assurance
 * Tests Spring Security configuration and role-based access.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security Integration Tests")
class SecurityTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // ==================== Authentication Tests ====================
    
    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {
        
        @Test
        @DisplayName("Should show login page for unauthenticated users")
        void loginPage_Accessible() throws Exception {
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk());
        }
        
        @Test
        @DisplayName("Should redirect to login for protected admin resources")
        void protectedAdminResource_RedirectsToLogin() throws Exception {
            mockMvc.perform(get("/admin/dashboard"))
                    .andExpect(status().is3xxRedirection());
        }
        
        @Test
        @DisplayName("Should redirect to login for protected student resources")
        void protectedStudentResource_RedirectsToLogin() throws Exception {
            mockMvc.perform(get("/student/dashboard"))
                    .andExpect(status().is3xxRedirection());
        }
        
        @Test
        @DisplayName("Should allow access to public home page")
        void homePage_Accessible() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk());
        }
    }
    
    // ==================== Authorization Tests ====================
    
    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {
        
        @Test
        @DisplayName("Admin should access admin dashboard")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void adminDashboard_WithAdminRole_Allowed() throws Exception {
            mockMvc.perform(get("/admin/dashboard"))
                    .andExpect(status().isOk());
        }
        
        @Test
        @DisplayName("Student should not access admin dashboard")
        @WithMockUser(username = "student", roles = {"STUDENT"})
        void adminDashboard_WithStudentRole_Forbidden() throws Exception {
            mockMvc.perform(get("/admin/dashboard"))
                    .andExpect(status().isForbidden());
        }
        
        @Test
        @DisplayName("Student should access student dashboard")
        @WithMockUser(username = "student", roles = {"STUDENT"})
        void studentDashboard_WithStudentRole_Allowed() throws Exception {
            mockMvc.perform(get("/student/dashboard"))
                    .andExpect(status().isOk());
        }
        
        @Test
        @DisplayName("Admin should also access student dashboard (per security config)")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void studentDashboard_WithAdminRole_Allowed() throws Exception {
            // Security config allows both ADMIN and STUDENT to access /student/**
            mockMvc.perform(get("/student/dashboard"))
                    .andExpect(status().isOk());
        }
    }
    
    // ==================== CSRF Protection Tests ====================
    
    @Nested
    @DisplayName("CSRF Protection Tests")
    class CsrfTests {
        
        @Test
        @DisplayName("POST without CSRF should fail")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void postWithoutCsrf_Fails() throws Exception {
            mockMvc.perform(post("/admin/books/add")
                    .param("title", "Test"))
                    .andExpect(status().isForbidden());
        }
        
        @Test
        @DisplayName("POST with CSRF should not return 403 for CSRF")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void postWithCsrf_CsrfAccepted() throws Exception {
            // With CSRF token, the request should not be rejected for CSRF reasons
            // It may fail for other validation reasons, but not CSRF
            mockMvc.perform(post("/admin/categories/add")
                    .with(csrf())
                    .param("name", "Test Category")
                    .param("description", "Test Description"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        // Should not be 403 (CSRF rejected) - could be 200, 302, or 400
                        assertTrue(status != 403 || 
                            !result.getResponse().getContentAsString().contains("CSRF"),
                            "Request should not be rejected for CSRF");
                    });
        }
    }
    
    // ==================== Logout Tests ====================
    
    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {
        
        @Test
        @DisplayName("Logout should invalidate session")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void logout_InvalidatesSession() throws Exception {
            mockMvc.perform(logout())
                    .andExpect(status().is3xxRedirection())
                    .andExpect(unauthenticated());
        }
    }
    
    // ==================== Static Resources Tests ====================
    
    @Nested
    @DisplayName("Static Resource Tests")
    class StaticResourceTests {
        
        @Test
        @DisplayName("CSS resources path should not require authentication")
        void cssResources_NotRedirectedToLogin() throws Exception {
            // Static resources should either return OK or NOT_FOUND, but not redirect to login
            mockMvc.perform(get("/css/style.css"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status == 200 || status == 404,
                            "Static resources should not redirect to login");
                    });
        }
        
        @Test
        @DisplayName("JS resources path should not require authentication")
        void jsResources_NotRedirectedToLogin() throws Exception {
            // Static resources should either return OK or NOT_FOUND, but not redirect to login
            mockMvc.perform(get("/js/main.js"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status == 200 || status == 404,
                            "Static resources should not redirect to login");
                    });
        }
    }
    
    // ==================== Password Encoding Tests ====================
    
    @Nested
    @DisplayName("Password Encoding Tests")
    class PasswordEncodingTests {
        
        @Test
        @DisplayName("Password encoder should hash passwords")
        void passwordEncoder_HashesPassword() {
            String rawPassword = "testPassword123";
            String encoded = passwordEncoder.encode(rawPassword);
            
            // Should be different from raw
            assertNotEquals(rawPassword, encoded);
        }
        
        @Test
        @DisplayName("Password encoder should match correctly")
        void passwordEncoder_MatchesCorrectPassword() {
            String rawPassword = "testPassword123";
            String encoded = passwordEncoder.encode(rawPassword);
            
            assertTrue(passwordEncoder.matches(rawPassword, encoded));
        }
        
        @Test
        @DisplayName("Password encoder should not match wrong password")
        void passwordEncoder_RejectsWrongPassword() {
            String rawPassword = "testPassword123";
            String encoded = passwordEncoder.encode(rawPassword);
            
            assertFalse(passwordEncoder.matches("wrongPassword", encoded));
        }
        
        @Test
        @DisplayName("Each encoding should be unique due to salt")
        void passwordEncoder_UniqueSalt() {
            String password = "samePassword";
            String encoded1 = passwordEncoder.encode(password);
            String encoded2 = passwordEncoder.encode(password);
            
            // Different encodings due to salt
            assertNotEquals(encoded1, encoded2);
            
            // But both should match the original
            assertTrue(passwordEncoder.matches(password, encoded1));
            assertTrue(passwordEncoder.matches(password, encoded2));
        }
    }
}
