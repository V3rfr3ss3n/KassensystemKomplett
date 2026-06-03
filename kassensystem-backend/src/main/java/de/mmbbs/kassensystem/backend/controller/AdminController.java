package de.mmbbs.kassensystem.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {
    @GetMapping({"/admin", "/admin/"})
    public String adminIndex() {
        return "redirect:/admin/index.html";
    }
}
