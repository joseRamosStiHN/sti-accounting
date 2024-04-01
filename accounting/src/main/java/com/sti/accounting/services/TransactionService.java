package com.sti.accounting.services;

import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.entities.TransactionDetailEntity;
import com.sti.accounting.entities.TransactionEntity;
import com.sti.accounting.models.TransactionRequest;
import com.sti.accounting.repositories.ITransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionService {

    private final ITransactionRepository transactionRepository;
    public TransactionService(ITransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }



    //operacion basica para agregar una transaccion
    //otras validaciones que se pueden realizar, es antes de ingresar al controller validar que existen las cuentas
    //o realizar la validacion dentro del Stream y rechazar toda la operacion
    @Transactional
    public void AddTransaction(TransactionRequest model){

        TransactionEntity  transactionEntity = new TransactionEntity();

        transactionEntity.setReference(model.getReference());
        transactionEntity.setDocumentType(model.getDocumentType());
        transactionEntity.setExchangeRate(model.getExchangeRate());

        List<TransactionDetailEntity> detail = model.getDetail().parallelStream().map(x -> {
            TransactionDetailEntity dto = new TransactionDetailEntity();
            dto.setTransaction(transactionEntity);
            dto.setAccount(new AccountEntity(x.getAccountId()));
            dto.setAmount(x.getAmount());
            return dto;
        }).toList();

        transactionEntity.setTransactionDetail(detail);

        transactionRepository.save(transactionEntity);
    }



}
