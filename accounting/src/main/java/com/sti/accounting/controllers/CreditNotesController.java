package com.sti.accounting.controllers;

import com.sti.accounting.models.CreditNotesRequest;
import com.sti.accounting.models.CreditNotesResponse;
import com.sti.accounting.models.DebitNotesRequest;
import com.sti.accounting.models.DebitNotesResponse;
import com.sti.accounting.services.CreditNotesService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/credit-notes")
public class CreditNotesController {

    private final CreditNotesService creditNotesService;

    public CreditNotesController(CreditNotesService creditNotesService) {
        this.creditNotesService = creditNotesService;
    }

    @GetMapping
    public List<CreditNotesResponse> getAllCreditNotes() {
        return creditNotesService.getAllCreditNotes();
    }

    @GetMapping("/{id}")
    public CreditNotesResponse getCreditNoteById(@PathVariable Long id) {
        return creditNotesService.getCreditNoteById(id);
    }

    @GetMapping("/by-transaction/{transactionId}")
    public List<CreditNotesResponse> getCreditNoteByTransactionId(@PathVariable Long transactionId) {
        return creditNotesService.getCreditNoteByTransactionId(transactionId);
    }

    @PostMapping
    public CreditNotesResponse createCreditNote(@Validated @RequestBody CreditNotesRequest creditNotesRequest) {
        return creditNotesService.createCreditNote(creditNotesRequest);
    }

    @PutMapping("/confirm-credit-notes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeCreditNoteStatus(@RequestBody List<Long> debitNoteIds) {
        creditNotesService.changeCreditNoteStatus(debitNoteIds);
    }
}
