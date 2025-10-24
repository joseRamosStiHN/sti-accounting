package com.sti.accounting.controllers;

import com.sti.accounting.models.AccountingAdjustmentRequest;
import com.sti.accounting.models.AccountingAdjustmentResponse;
import com.sti.accounting.models.DebitNotesRequest;
import com.sti.accounting.models.DebitNotesResponse;
import com.sti.accounting.services.DebitNotesService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/debit-notes")
public class DebitNotesController {

    private final DebitNotesService debitNotesService;

    public DebitNotesController(DebitNotesService debitNotesService) {
        this.debitNotesService = debitNotesService;
    }

    @GetMapping
    public List<DebitNotesResponse> getAllDebitNotes() {
        return debitNotesService.getAllDebitNotes();
    }

    @GetMapping("/{id}")
    public DebitNotesResponse getDebitNoteById(@PathVariable Long id) {
        return debitNotesService.getDebitNoteById(id);
    }

    @GetMapping("/by-transaction/{transactionId}")
    public List<DebitNotesResponse> getDebitNoteByTransactionId(@PathVariable Long transactionId) {
        return debitNotesService.getDebitNoteByTransactionId(transactionId);
    }

    @PostMapping
    public DebitNotesResponse createDebitNote(@Validated @RequestBody DebitNotesRequest debitNotesRequest) {
        return debitNotesService.createDebitNote(debitNotesRequest);
    }

    @PutMapping("/{id}")
    public DebitNotesResponse updateDebitNote(
            @PathVariable Long id,
            @Validated @RequestBody DebitNotesRequest debitNotesRequest
    ) {
        return debitNotesService.updateDebitNote(id, debitNotesRequest);
    }


    @PutMapping("/confirm-debit-notes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeDebitNoteStatus(@RequestBody List<Long> debitNoteId) {
        debitNotesService.changeDebitNoteStatus(debitNoteId);
    }
}
