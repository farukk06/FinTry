package com.fintry.service;

import com.fintry.entity.User;
import com.fintry.entity.VirtualAccount;
import com.fintry.repository.UserRepository;
import com.fintry.repository.VirtualAccountRepository;
import org.springframework.stereotype.Service;
import com.fintry.exception.BusinessRuleException;
import com.fintry.exception.ResourceNotFoundException;

import java.math.BigDecimal;

@Service
public class VirtualAccountService {

    private final VirtualAccountRepository virtualAccountRepository;
    private final UserRepository userRepository;

    public VirtualAccountService(VirtualAccountRepository virtualAccountRepository, UserRepository userRepository) {
        this.virtualAccountRepository = virtualAccountRepository;
        this.userRepository = userRepository;
    }

    public VirtualAccount createVirtualAccount(Long userId, BigDecimal balance) {
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

        return virtualAccountRepository.save(virtualAccount);
    }

    public VirtualAccount getByUserId(Long userId) {
        return virtualAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Virtual account not found"));
    }
}