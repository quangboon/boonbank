package com.boon.bank.service.impl;

import com.boon.bank.dto.request.TransactionRequest;
import com.boon.bank.dto.response.TransactionResponse;
import com.boon.bank.entity.Account;
import com.boon.bank.entity.Transaction;
import com.boon.bank.entity.enums.TransactionType;
import com.boon.bank.exception.BusinessException;
import com.boon.bank.exception.ErrorCode;
import com.boon.bank.exception.NotFoundException;
import com.boon.bank.mapper.TransactionMapper;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.TransactionRepository;
import com.boon.bank.security.SecurityUtil;
import com.boon.bank.service.FraudCheckEvent;
import com.boon.bank.service.TransactionService;
import com.boon.bank.service.fee.FeeService;
import com.boon.bank.specification.TransactionSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Slf4j
@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository txnRepo;
    private final AccountRepository acctRepo;
    private final TransactionMapper mapper;
    private final FeeService feeService;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurityUtil securityUtil;
    private final CacheManager cacheManager;
    private final BigDecimal minAmount;
    private final String timezone;

    public TransactionServiceImpl(TransactionRepository txnRepo, AccountRepository acctRepo,
                                  TransactionMapper mapper, FeeService feeService,
                                  ApplicationEventPublisher eventPublisher, SecurityUtil securityUtil,
                                  CacheManager cacheManager,
                                  @Value("${app.transaction.min-amount}") BigDecimal minAmount,
                                  @Value("${app.timezone:Asia/Ho_Chi_Minh}") String timezone) {
        this.txnRepo = txnRepo;
        this.acctRepo = acctRepo;
        this.mapper = mapper;
        this.feeService = feeService;
        this.eventPublisher = eventPublisher;
        this.securityUtil = securityUtil;
        this.cacheManager = cacheManager;
        this.minAmount = minAmount;
        this.timezone = timezone;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> findAll(Pageable pageable) {
        if (!securityUtil.isAdmin()) {
            Long custId = securityUtil.currentCustomerId();
            if (custId == null) throw new BusinessException(ErrorCode.NO_CUSTOMER, "No customer linked");
            var acctIds = acctRepo.findAccountIdsByCustomerId(custId);
            return txnRepo.findByFromAccountIdInOrToAccountIdIn(acctIds, acctIds, pageable)
                    .map(mapper::toResponse);
        }
        return txnRepo.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> search(TransactionType type, BigDecimal amountMin, BigDecimal amountMax,
                                             OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        var spec = Specification.where(TransactionSpec.byType(type))
                .and(TransactionSpec.amountBetween(amountMin, amountMax))
                .and(TransactionSpec.dateBetween(from, to));
        return txnRepo.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id) {
        var txn = txnRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        if (!securityUtil.isAdmin()) {
            Long custId = securityUtil.currentCustomerId();
            if (custId == null) throw new BusinessException(ErrorCode.NO_CUSTOMER, "No customer linked");
            var acctIds = acctRepo.findAccountIdsByCustomerId(custId);
            boolean owns = (txn.getFromAccount() != null && acctIds.contains(txn.getFromAccount().getId()))
                    || (txn.getToAccount() != null && acctIds.contains(txn.getToAccount().getId()));
            if (!owns) throw new NotFoundException("Transaction not found");
        }
        return mapper.toResponse(txn);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> findByAccount(Long accountId, Pageable pageable) {
        if (!securityUtil.isAdmin()) {
            Long custId = securityUtil.currentCustomerId();
            var acct = acctRepo.findById(accountId)
                    .orElseThrow(() -> new NotFoundException("Account not found"));
            if (!acct.getCustomer().getId().equals(custId))
                throw new BusinessException(ErrorCode.FORBIDDEN, "Not your account", HttpStatus.FORBIDDEN);
        }
        return txnRepo.findByFromAccountIdOrToAccountId(accountId, accountId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public BigDecimal previewFee(TransactionType type, BigDecimal amount) {
        return feeService.resolve(type).calculate(amount);
    }

    @Override
    @Transactional
    public TransactionResponse execute(TransactionRequest req) {
        if (!securityUtil.isAdmin() && req.type() != TransactionType.TRANSFER)
            throw new BusinessException(ErrorCode.TXN_TYPE_FORBIDDEN,
                    "Only transfer allowed", HttpStatus.FORBIDDEN);

        if (req.amount().compareTo(minAmount) < 0)
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Min amount is " + minAmount.toPlainString() + " VND");

        // resolve toAccountNumber -> toAccountId
        var resolved = resolveToAccount(req);

        if (resolved.idempotencyKey() != null) {
            var existing = txnRepo.findByIdempotencyKey(resolved.idempotencyKey());
            if (existing.isPresent()) return mapper.toResponse(existing.get());
        }
        try {
            return switch (resolved.type()) {
                case DEPOSIT -> doDeposit(resolved);
                case WITHDRAWAL -> doWithdrawal(resolved);
                case TRANSFER -> doTransfer(resolved);
            };
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // concurrent request with same idempotency key — return existing
            if (resolved.idempotencyKey() != null) {
                var existing = txnRepo.findByIdempotencyKey(resolved.idempotencyKey());
                if (existing.isPresent()) return mapper.toResponse(existing.get());
            }
            throw e;
        }
    }

    private TransactionRequest resolveToAccount(TransactionRequest req) {
        if (req.toAccountId() != null || req.toAccountNumber() == null) return req;
        var acct = acctRepo.findByAccountNumber(req.toAccountNumber())
                .orElseThrow(() -> new NotFoundException("Account " + req.toAccountNumber() + " not found"));
        return new TransactionRequest(req.type(), req.fromAccountId(), acct.getId(),
                req.toAccountNumber(), req.amount(), req.location(), req.description(), req.idempotencyKey());
    }

    private TransactionResponse doDeposit(TransactionRequest req) {
        var acct = lockAccount(req.toAccountId(), "toAccountId required for deposit");
        acct.checkActive();
        var fee = feeService.resolve(TransactionType.DEPOSIT).calculate(req.amount());
        acct.credit(req.amount().subtract(fee));
        var txn = buildTxn(req, null, acct, fee);
        log.info("Deposit {} to acct={}", req.amount(), acct.getAccountNumber());
        return saveAndPublish(txn);
    }

    private TransactionResponse doWithdrawal(TransactionRequest req) {
        var acct = lockAccount(req.fromAccountId(), "fromAccountId required for withdrawal");
        acct.checkActive();
        checkOwnership(acct);
        acct.checkLimit(req.amount());
        checkCustomerTypeLimit(acct, req.amount());
        var fee = feeService.resolve(TransactionType.WITHDRAWAL).calculate(req.amount());
        acct.debit(req.amount().add(fee));
        var txn = buildTxn(req, acct, null, fee);
        log.info("Withdrawal {} from acct={}", req.amount(), acct.getAccountNumber());
        return saveAndPublish(txn);
    }

    private TransactionResponse doTransfer(TransactionRequest req) {
        if (req.fromAccountId() == null || req.toAccountId() == null)
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Both accounts required for transfer");
        if (req.fromAccountId().equals(req.toAccountId()))
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Cannot transfer to same account");

        Long first = Math.min(req.fromAccountId(), req.toAccountId());
        Long second = Math.max(req.fromAccountId(), req.toAccountId());
        Account a1 = acctRepo.findByIdForUpdate(first).orElseThrow(() -> new NotFoundException("Account not found"));
        Account a2 = acctRepo.findByIdForUpdate(second).orElseThrow(() -> new NotFoundException("Account not found"));

        Account from = req.fromAccountId().equals(first) ? a1 : a2;
        Account to = req.fromAccountId().equals(first) ? a2 : a1;

        from.checkActive();
        to.checkActive();
        checkOwnership(from);
        from.checkLimit(req.amount());
        checkCustomerTypeLimit(from, req.amount());
        var fee = feeService.resolve(TransactionType.TRANSFER).calculate(req.amount());
        from.debit(req.amount().add(fee));
        to.credit(req.amount());

        var txn = buildTxn(req, from, to, fee);
        log.info("Transfer {} -> {} amt={}", from.getAccountNumber(), to.getAccountNumber(), req.amount());
        return saveAndPublish(txn);
    }

    private void checkOwnership(Account acct) {
        if (!securityUtil.isAdmin()) {
            Long custId = securityUtil.currentCustomerId();
            if (!acct.getCustomer().getId().equals(custId))
                throw new BusinessException(ErrorCode.FORBIDDEN, "Not your account", HttpStatus.FORBIDDEN);
        }
    }

    private Account lockAccount(Long id, String errMsg) {
        if (id == null) throw new BusinessException(ErrorCode.INVALID_REQUEST, errMsg);
        return acctRepo.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Account not found"));
    }

    private Transaction buildTxn(TransactionRequest req, Account from, Account to, BigDecimal fee) {
        return Transaction.builder()
                .fromAccount(from).toAccount(to)
                .type(req.type()).amount(req.amount()).fee(fee)
                .location(req.location()).description(req.description())
                .idempotencyKey(req.idempotencyKey()).build();
    }

    private TransactionResponse saveAndPublish(Transaction txn) {
        var saved = txnRepo.save(txn);

        var acctCache = cacheManager.getCache("accounts");
        if (acctCache != null) {
            if (saved.getFromAccount() != null) acctCache.evict(saved.getFromAccount().getId());
            if (saved.getToAccount() != null) acctCache.evict(saved.getToAccount().getId());
        }
        var statsCache = cacheManager.getCache("statistics");
        if (statsCache != null) statsCache.clear();

        eventPublisher.publishEvent(new FraudCheckEvent(
                saved.getId(),
                saved.getFromAccount() != null ? saved.getFromAccount().getId() : saved.getToAccount().getId(),
                saved.getAmount(),
                saved.getType()
        ));
        return mapper.toResponse(saved);
    }

    private void checkCustomerTypeLimit(Account acct, BigDecimal amount) {
        var custType = acct.getCustomer().getCustomerType();
        if (amount.compareTo(custType.getTxnLimit()) > 0)
            throw new BusinessException(ErrorCode.TYPE_LIMIT_EXCEEDED,
                    "Amount exceeds " + custType.getName() + " txn limit");

        var zone = java.time.ZoneId.of(timezone);
        var todayStart = java.time.LocalDate.now(zone).atStartOfDay(zone).toOffsetDateTime();
        var dailyTotal = txnRepo.sumAmountByAccountSince(acct.getId(), todayStart);
        if (dailyTotal.add(amount).compareTo(custType.getDailyLimit()) > 0)
            throw new BusinessException(ErrorCode.DAILY_LIMIT_EXCEEDED,
                    "Exceeds " + custType.getName() + " daily limit");

        var todayCount = txnRepo.countByAccountSince(acct.getId(), todayStart);
        if (todayCount >= custType.getMaxTxnPerDay())
            throw new BusinessException(ErrorCode.TXN_LIMIT_EXCEEDED,
                    "Max " + custType.getMaxTxnPerDay() + " txn/day for " + custType.getName());
    }
}
