package com.nationalteammanager.controller;

import com.nationalteammanager.config.AppUserPrincipal;
import com.nationalteammanager.service.StatisticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final StatisticsService statisticsService;

    public DashboardController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AppUserPrincipal user, Model model) {
        model.addAttribute("dashboard", statisticsService.getDashboard(user.getId()));
        return "dashboard";
    }

    @GetMapping("/statistics")
    public String statistics(@AuthenticationPrincipal AppUserPrincipal user, Model model) {
        model.addAttribute("stats", statisticsService.getOverallStatistics(user.getId()));
        return "statistics";
    }
}
