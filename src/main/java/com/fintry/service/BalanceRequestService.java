package com.fintry.service;

import jakarta.transaction.Transactional;
import com.fintry.entity.BalanceRequest;
import com.fintry.entity.BalanceRequestStatus;
import com.fintry.entity.VirtualAccount;
import com.fintry.repository.BalanceRequestRepository;
import com.fintry.repository.VirtualAccountRepository;
import com.fintry.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fintry.exception.BusinessRuleException;
import com.fintry.exception.ResourceNotFoundException;
import com.fintry.dto.CreateBalanceRequest;
import com.fintry.dto.BalanceRequestResponse;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BalanceRequestService {

    private final BalanceRequestRepository balanceRequestRepository;
    private final VirtualAccountRepository virtualAccountRepository;
    private final UserRepository userRepository;
    private final FinancialLocks financialLocks;

    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("@accountAccess.owns(#request.userId)")
    public BalanceRequestResponse createRequest(CreateBalanceRequest request) {
        if (request.getUserId() == null) {
            throw new ResourceNotFoundException("User not found");
        }
        var user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BalanceRequest balanceRequest = BalanceRequest.builder()
                .user(user)
                .requestedAmount(FinancialPolicy.amount(request.getRequestedAmount()))
                .status(BalanceRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        BalanceRequest savedRequest =
                balanceRequestRepository.save(balanceRequest);

        return toResponse(savedRequest);
    }

    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public BalanceRequestResponse approveRequest(Long requestId) {
        // Global order: request (approval only) -> account -> portfolio. Never reverse it.
        financialLocks.configureTimeout();
        BalanceRequest request = balanceRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Talep bulunamadı"));

        if (request.getStatus() != BalanceRequestStatus.PENDING) {
            throw new BusinessRuleException("Bu talep zaten işlenmiş");
        }

        VirtualAccount account = virtualAccountRepository
                .findByUserIdForUpdate(request.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Virtual account bulunamadı"));

        account.setBalance(
                FinancialPolicy.balance(account.getBalance().add(FinancialPolicy.amount(request.getRequestedAmount())))
        );

        request.setStatus(BalanceRequestStatus.APPROVED);

        virtualAccountRepository.save(account);

        BalanceRequest savedRequest = balanceRequestRepository.save(request);

        return toResponse(savedRequest);
    }
    @org.springframework.security.access.prepost.PreAuthorize("@accountAccess.owns(#userId)")
    public java.util.List<BalanceRequestResponse> getByUserId(Long userId) {
        return balanceRequestRepository.findByUserId(userId).stream().map(this::toResponse).toList();
    }
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public java.util.List<BalanceRequestResponse> pendingRequests() {
        return balanceRequestRepository.findByStatus(BalanceRequestStatus.PENDING).stream().map(this::toResponse).toList();
    }
    private BalanceRequestResponse toResponse(BalanceRequest request) {
        return BalanceRequestResponse.builder()
                .id(request.getId())
                .userId(request.getUser().getId())
                .requestedAmount(request.getRequestedAmount())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
