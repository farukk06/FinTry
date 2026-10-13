package com.fintry.controller;

import com.fintry.dto.PortfolioResponse;
import com.fintry.service.PortfolioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/portfolio")
public class PortfolioController {

    @org.springframework.beans.factory.annotation.Autowired
    private com.fintry.security.AccountAccess access;

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/me")
    public List<PortfolioResponse> me() { return portfolioService.getUserPortfolio(access.userId()); }

    @GetMapping("/user/{userId}")
    public List<PortfolioResponse> getUserPortfolio(@PathVariable Long userId) {
        return portfolioService.getUserPortfolio(userId);
    }
}