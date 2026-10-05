package com.fintry.controller;

import com.fintry.dto.CreateBalanceRequest;
import com.fintry.dto.BalanceRequestResponse;
import jakarta.validation.Valid;
import com.fintry.service.BalanceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/balance-requests")
@RequiredArgsConstructor
public class BalanceRequestController {

    private final BalanceRequestService balanceRequestService;

    @PostMapping
    public BalanceRequestResponse createRequest(@Valid @RequestBody CreateBalanceRequest request) {
        return balanceRequestService.createRequest(request);
    }

    @PutMapping("/{id}/approve")
    public BalanceRequestResponse approveRequest(@PathVariable Long id) {
        return balanceRequestService.approveRequest(id);
    }
}