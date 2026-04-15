package com.fintry.controller;

import com.fintry.dto.PortfolioResponse;
import com.fintry.service.PortfolioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/user/{userId}")
    public List<PortfolioResponse> getUserPortfolio(@PathVariable Long userId) {
        return portfolioService.getUserPortfolio(userId);
    }
}