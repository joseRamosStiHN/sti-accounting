package com.sti.accounting.services;

import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.entities.Constant;
import com.sti.accounting.models.BalancesRequest;
import com.sti.accounting.repositories.IAccountRepository;
import com.sti.accounting.repositories.IBalancesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;

@Service
public class BalancesService {

    private static final Logger logger = LoggerFactory.getLogger(BalancesService.class);

    private final IBalancesRepository iBalancesRepository;
    private final IAccountRepository iAccountRepository;

    public BalancesService(IBalancesRepository iBalancesRepository, IAccountRepository iAccountRepository) {
        this.iBalancesRepository = iBalancesRepository;
        this.iAccountRepository = iAccountRepository;
    }


    public List<BalancesRequest> getAllBalances() {
        List<BalancesEntity> entities = this.iBalancesRepository.findAll();
        return entities.stream().map(BalancesEntity::entityToRequest).collect(Collectors.toList());
    }

    public BalancesRequest getById(Long id) {
        logger.trace("balance request with id {}", id);
        BalancesEntity balancesEntity = this.iBalancesRepository.findById(id).orElseThrow(
                () -> new BadRequestException(String.format(Constant.NOT_BALANCE, id))
        );
        return balancesEntity.entityToRequest();
    }

    public BalancesEntity createBalances(BalancesRequest balancesRequest) {
        logger.info("creating balance");
        try {
            AccountEntity account = iAccountRepository.findById(balancesRequest.getAccountId()).orElseThrow(
                    () -> new BadRequestException(String.format("The specified account %s does not exist", balancesRequest.getAccountId()))
            );


            BalancesEntity newBalance = new BalancesEntity();
            newBalance.setAccount(account);
            newBalance.setInitialBalance(balancesRequest.getInitialBalance());
            newBalance.setIsActual(balancesRequest.getIsActual());
            return iBalancesRepository.save(newBalance);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error creating balance: " + e.getMessage());
        }
    }

    public BalancesEntity updateBalance(Long id, BalancesRequest balancesRequest) {
        logger.info("Updating balance with ID: {}", id);
        try {
            BalancesEntity existingBalance = this.iBalancesRepository.findById(id).orElseThrow(
                    () -> new BadRequestException(String.format(Constant.NOT_BALANCE, id))
            );

            AccountEntity account = iAccountRepository.findById(balancesRequest.getAccountId()).orElseThrow(
                    () -> new BadRequestException(String.format("The specified account %s does not exist", balancesRequest.getAccountId()))
            );

            existingBalance.setAccount(account);
            existingBalance.setInitialBalance(balancesRequest.getInitialBalance());
            existingBalance.setCreateAtDate(LocalDateTime.now());
            existingBalance.setIsActual(balancesRequest.getIsActual());
            return iBalancesRepository.save(existingBalance);


        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error updating balance: " + e.getMessage());
        }
    }

    public void deleteBalance(Long id) {
        logger.info("delete balance");
        BalancesEntity existingBalance = this.iBalancesRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(Constant.NOT_BALANCE, id))
        );

        iBalancesRepository.deleteById(id);
    }
}
