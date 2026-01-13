package com.library.management.controller;

import com.library.management.model.Notification;
import com.library.management.model.User;
import com.library.management.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for managing user notifications.
 */
@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * View all notifications.
     */
    @GetMapping
    public String viewNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Page<Notification> notifications = notificationService.getUserNotifications(
            user, PageRequest.of(page, 20)
        );

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        model.addAttribute("user", user);

        return "notifications/list";
    }

    /**
     * Get unread notification count (for AJAX).
     */
    @GetMapping("/unread-count")
    @ResponseBody
    public long getUnreadCount(@AuthenticationPrincipal User user) {
        return notificationService.getUnreadCount(user);
    }

    /**
     * Get recent notifications (for dropdown).
     */
    @GetMapping("/recent")
    @ResponseBody
    public List<Notification> getRecentNotifications(@AuthenticationPrincipal User user) {
        return notificationService.getRecentNotifications(user);
    }

    /**
     * Mark notification as read.
     */
    @PostMapping("/{id}/read")
    public String markAsRead(
            @PathVariable Long id,
            @RequestHeader(value = "Referer", required = false) String referer,
            RedirectAttributes redirectAttributes) {

        notificationService.markAsRead(id);
        redirectAttributes.addFlashAttribute("successMessage", "Notification marked as read");

        return "redirect:" + (referer != null ? referer : "/notifications");
    }

    /**
     * Mark all notifications as read.
     */
    @PostMapping("/mark-all-read")
    public String markAllAsRead(
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {

        int count = notificationService.markAllAsRead(user);
        redirectAttributes.addFlashAttribute("successMessage", count + " notifications marked as read");

        return "redirect:/notifications";
    }

    /**
     * Delete a notification.
     */
    @PostMapping("/{id}/delete")
    public String deleteNotification(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        notificationService.deleteNotification(id);
        redirectAttributes.addFlashAttribute("successMessage", "Notification deleted");

        return "redirect:/notifications";
    }
}
