package com.sti.accounting.services;

import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.models.BalancesResponse;
import com.sti.accounting.models.Constant;
import com.sti.accounting.models.BalancesRequest;
import com.sti.accounting.repositories.IAccountRepository;
import com.sti.accounting.repositories.IBalancesRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BalancesService {

    private static final Logger logger = LoggerFactory.getLogger(BalancesService.class);

    private final IBalancesRepository iBalancesRepository;
    private final IAccountRepository iAccountRepository;
    private final AuthService authService;
    public BalancesService(IBalancesRepository iBalancesRepository, IAccountRepository iAccountRepository, AuthService authService) {
        this.iBalancesRepository = iBalancesRepository;
        this.iAccountRepository = iAccountRepository;

        this.authService = authService;
    }

//    private String getTenantId() {
//        return TenantContext.getCurrentTenant();
//    }

    public List<BalancesResponse> getAllBalances() {
        String tenantId = authService.getTenantId();
        return this.iBalancesRepository.findAll().stream().filter(balances -> balances.getTenantId().equals(tenantId)).map(this::toResponse).toList();

    }

    public BalancesResponse getById(Long id) {
        logger.trace("balance request with id {}", id);
        String tenantId = authService.getTenantId();

        BalancesEntity balancesEntity = this.iBalancesRepository.findById(id).filter(balances -> balances.getTenantId().equals(tenantId)).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(Constant.NOT_BALANCE, id))
        );
        return toResponse(balancesEntity);
    }

    public BalancesResponse createBalance(BalancesRequest balancesRequest) {
        logger.info("creating balance");
        BalancesEntity balanceEntity = new BalancesEntity();
        String tenantId = authService.getTenantId();

        AccountEntity accountEntity = iAccountRepository.findById(balancesRequest.getAccountId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("No account were found with the id %s", balancesRequest.getAccountId())));


        balanceEntity.setAccount(accountEntity);
        balanceEntity.setTypicalBalance(balancesRequest.getTypicalBalance());
        balanceEntity.setInitialBalance(balancesRequest.getInitialBalance());
        balanceEntity.setIsCurrent(balancesRequest.getIsCurrent());
        balanceEntity.setTenantId(tenantId);
        iBalancesRepository.save(balanceEntity);

        return toResponse(balanceEntity);
    }

    public BalancesResponse updateBalance(Long id, BalancesRequest balancesRequest) {
        logger.info("Updating balance with ID: {}", id);
        String tenantId = authService.getTenantId();

        if (balancesRequest.getInitialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The initial account balance cannot be negative");
        }

        BalancesEntity existingBalance = this.iBalancesRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(Constant.NOT_BALANCE, id))
        );

        AccountEntity accountEntity = iAccountRepository.findById(balancesRequest.getAccountId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("No account were found with the id %s", balancesRequest.getAccountId())));


        existingBalance.setAccount(accountEntity);
        existingBalance.setTypicalBalance(balancesRequest.getTypicalBalance());
        existingBalance.setInitialBalance(balancesRequest.getInitialBalance());
        existingBalance.setCreateAtDate(LocalDateTime.now());
        existingBalance.setIsCurrent(balancesRequest.getIsCurrent());
        existingBalance.setTenantId(tenantId);
        iBalancesRepository.save(existingBalance);
        return toResponse(existingBalance);

    }

    private BalancesResponse toResponse(BalancesEntity balancesEntity) {
        BalancesResponse balancesResponse = new BalancesResponse();
        balancesResponse.setId(balancesEntity.getId());
        balancesResponse.setAccountId(balancesEntity.getAccount().getId());
        balancesResponse.setTypicalBalance(balancesEntity.getTypicalBalance());
        balancesResponse.setInitialBalance(balancesEntity.getInitialBalance());
        balancesResponse.setIsCurrent(balancesEntity.getIsCurrent());
        balancesResponse.setCreateAtDate(balancesEntity.getCreateAtDate());
        balancesResponse.setClosingDate(balancesEntity.getClosingDate());
        return balancesResponse;

    }

}
