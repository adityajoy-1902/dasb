package com.example.dashboard.controller;

import com.example.dashboard.model.Application;
import com.example.dashboard.service.YamlParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private YamlParserService yamlParserService;

    @GetMapping("/")
    public String dashboard(Model model) {
        List<Application> applications = yamlParserService.parseYaml();
        model.addAttribute("applications", applications);
        return "dashboard";
    }
} 