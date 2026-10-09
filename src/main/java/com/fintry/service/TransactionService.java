package com.fintry.service;

import com.fintry.dto.TradeRequest;
import com.fintry.entity.*;
import com.fintry.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.fintry.entity.TransactionType;
import com.fintry.exception.ResourceNotFoundException;
import com.fintry.exception.InsufficientBalanceException;
import com.fintry.exception.InsufficientAssetException;
import com.fintry.dto.TransactionResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final InstrumentRepository instrumentRepository;
    private final VirtualAccountRepository virtualAccountRepository;
    private final PortfolioAssetRepository portfolioAssetRepository;
    private final FinancialLocks financialLocks;

    public TransactionService(TransactionRepository transactionRepository,
                              UserRepository userRepository,
                              InstrumentRepository instrumentRepository,
                              VirtualAccountRepository virtualAccountRepository,
                              PortfolioAssetRepository portfolioAssetRepository,
                              FinancialLocks financialLocks) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.instrumentRepository = instrumentRepository;
        this.virtualAccountRepository = virtualAccountRepository;
        this.portfolioAssetRepository = portfolioAssetRepository;
        this.financialLocks = financialLocks;
    }

    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("@accountAccess.owns(#request.userId)")
    public TransactionResponse buy(TradeRequest request) {
        FinancialPolicy.quantity(request.getQuantity());
        financialLocks.configureTimeout();
        VirtualAccount account = virtualAccountRepository.findByUserIdForUpdate(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Virtual account not found"));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Instrument instrument = instrumentRepository.findById(request.getInstrumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found"));

        BigDecimal quantity = FinancialPolicy.quantity(request.getQuantity());
        BigDecimal price = FinancialPolicy.price(instrument.getPrice());
        BigDecimal totalAmount = FinancialPolicy.total(price, quantity);

        if (account.getBalance().compareTo(totalAmount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        account.setBalance(FinancialPolicy.balance(account.getBalance().subtract(totalAmount)));

        PortfolioAsset asset = portfolioAssetRepository
                .findByUserIdAndInstrumentId(request.getUserId(), request.getInstrumentId())
                .orElse(null);

        if (asset == null) {
            asset = PortfolioAsset.builder()
                    .user(user)
                    .instrument(instrument)
                    .quantity(quantity)
                    .averagePrice(price)
                    .build();
        } else {
            BigDecimal oldQuantity = asset.getQuantity();
            BigDecimal oldAveragePrice = asset.getAveragePrice();
            BigDecimal newQuantity = FinancialPolicy.quantity(oldQuantity.add(quantity));

            BigDecimal oldTotal = oldQuantity.multiply(oldAveragePrice);
            BigDecimal newTotal = quantity.multiply(price);

            BigDecimal newAveragePrice = FinancialPolicy.average(oldTotal.add(newTotal), newQuantity);

            asset.setQuantity(newQuantity);
            asset.setAveragePrice(newAveragePrice);
        }

        Transaction transaction = Transaction.builder()
                .type(TransactionType.BUY)
                .quantity(quantity)
                .price(price)
                .totalAmount(totalAmount)
                .transactionTime(LocalDateTime.now())
                .user(user)
                .instrument(instrument)
                .build();

        portfolioAssetRepository.save(asset);
        virtualAccountRepository.save(account);

        Transaction savedTransaction = transactionRepository.save(transaction);

        return toResponse(savedTransaction);
    }

    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("@accountAccess.owns(#request.userId)")
    public TransactionResponse sell(TradeRequest request) {
        FinancialPolicy.quantity(request.getQuantity());
        financialLocks.configureTimeout();
        VirtualAccount account = virtualAccountRepository.findByUserIdForUpdate(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Virtual account not found"));
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Instrument instrument = instrumentRepository.findById(request.getInstrumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found"));

        PortfolioAsset asset = portfolioAssetRepository
                .findByUserIdAndInstrumentId(request.getUserId(), request.getInstrumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio asset not found"));

        BigDecimal quantity = FinancialPolicy.quantity(request.getQuantity());
        BigDecimal price = FinancialPolicy.price(instrument.getPrice());

        if (asset.getQuantity().compareTo(quantity) < 0) {
            throw new InsufficientAssetException("Insufficient asset quantity");
        }

        BigDecimal totalAmount = FinancialPolicy.total(price, quantity);
        BigDecimal remainingQuantity = asset.getQuantity().subtract(quantity);

        if (remainingQuantity.compareTo(BigDecimal.ZERO) == 0) {
            portfolioAssetRepository.delete(asset);
        } else {
            asset.setQuantity(remainingQuantity);
            portfolioAssetRepository.save(asset);
        }

        account.setBalance(FinancialPolicy.balance(account.getBalance().add(totalAmount)));
        virtualAccountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .type(TransactionType.SELL)
                .quantity(quantity)
                .price(price)
                .totalAmount(totalAmount)
                .transactionTime(LocalDateTime.now())
                .user(user)
                .instrument(instrument)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        return toResponse(savedTransaction);
    }

    @org.springframework.security.access.prepost.PreAuthorize("@accountAccess.owns(#userId)")
    public List<TransactionResponse> getTransactionsByUserId(Long userId) {
        return transactionRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }
    private TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .quantity(transaction.getQuantity())
                .price(transaction.getPrice())
                .totalAmount(transaction.getTotalAmount())
                .transactionTime(transaction.getTransactionTime())
                .instrumentId(transaction.getInstrument().getId())
                .instrumentSymbol(transaction.getInstrument().getSymbol())
                .build();
    }
}
