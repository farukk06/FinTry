package com.fintry.controller;

import com.fintry.entity.BalanceRequest;
import com.fintry.service.BalanceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/balance-requests")
@RequiredArgsConstructor
public class BalanceRequestController {

    private final BalanceRequestService balanceRequestService;

    @PostMapping
    public BalanceRequest createRequest(@RequestBody BalanceRequest request) {
        return balanceRequestService.createRequest(request);
    }

    @PutMapping("/{id}/approve")
    public BalanceRequest approveRequest(@PathVariable Long id) {
        return balanceRequestService.approveRequest(id);
    }
}