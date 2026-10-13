package com.fintry.service;

import com.fintry.entity.User;
import com.fintry.entity.VirtualAccount;
import com.fintry.repository.UserRepository;
import com.fintry.repository.VirtualAccountRepository;
import org.springframework.stereotype.Service;
import com.fintry.exception.BusinessRuleException;
import com.fintry.exception.ResourceNotFoundException;
import com.fintry.dto.VirtualAccountResponse;

import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VirtualAccountService {

    private final VirtualAccountRepository virtualAccountRepository;
    private final UserRepository userRepository;

    public VirtualAccountService(VirtualAccountRepository virtualAccountRepository, UserRepository userRepository) {
        this.virtualAccountRepository = virtualAccountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public VirtualAccountResponse createVirtualAccount(Long userId, BigDecimal balance) {
        balance = FinancialPolicy.balance(balance);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean accountExists = virtualAccountRepository.findByUserId(userId).isPresent();
        if (accountExists) {
            throw new BusinessRuleException("Virtual account already exists for this user");
        }

        VirtualAccount virtualAccount = VirtualAccount.builder()
                .balance(balance)
                .user(user)
                .build();

        VirtualAccount savedAccount =
                virtualAccountRepository.save(virtualAccount);

        return toResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    @org.springframework.security.access.prepost.PreAuthorize("@accountAccess.owns(#userId)")
    public VirtualAccountResponse getByUserId(Long userId) {

        VirtualAccount account = virtualAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Virtual account not found"));

        return toResponse(account);
    }

    private VirtualAccountResponse toResponse(VirtualAccount account) {
        return VirtualAccountResponse.builder()
                .id(account.getId())
                .userId(account.getUser().getId())
                .balance(account.getBalance())
                .build();
    }
}
