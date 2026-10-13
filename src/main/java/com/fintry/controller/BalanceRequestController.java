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

    @org.springframework.beans.factory.annotation.Autowired
    private com.fintry.security.AccountAccess access;

    private final BalanceRequestService balanceRequestService;

    @PostMapping
    public BalanceRequestResponse createRequest(@Valid @RequestBody com.fintry.dto.BalanceCommand command) {
        var request = new CreateBalanceRequest();
        request.setUserId(access.userId());
        request.setRequestedAmount(command.requestedAmount());
        return balanceRequestService.createRequest(request);
    }

    @GetMapping("/me")
    public java.util.List<BalanceRequestResponse> me() { return balanceRequestService.getByUserId(access.userId()); }

    @PutMapping("/{id}/approve")
    public BalanceRequestResponse approveRequest(@PathVariable Long id) {
        return balanceRequestService.approveRequest(id);
    }
}