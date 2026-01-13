package com.library.management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Home Controller for serving the main landing page.
 */
@Controller
public class HomeController {

    /**
     * Serves the main landing page.
     */
    @GetMapping("/")
    public String home() {
        return "index";
    }
}
