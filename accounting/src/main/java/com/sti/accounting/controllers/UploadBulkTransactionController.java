package com.sti.accounting.controllers;

import com.sti.accounting.models.BulkTransactionRequest;
import com.sti.accounting.models.BulkTransactionResponse;
import com.sti.accounting.models.UploadBulkTransactionResponse;
import com.sti.accounting.services.BulkAccountConfigService;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/upload-bulk-transaction")
public class UploadBulkTransactionController {

    private final BulkAccountConfigService bulkAccountConfigService;

    public UploadBulkTransactionController(BulkAccountConfigService bulkAccountConfigService) {
        this.bulkAccountConfigService = bulkAccountConfigService;
    }

    @PostMapping
    public UploadBulkTransactionResponse uploadBulkTransaction(@RequestPart("file") MultipartFile file, @RequestParam Long idConfig ) {
       return this.bulkAccountConfigService.excelToObject(file, idConfig);
    }

    @PostMapping("config")
    public BulkTransactionResponse createBulkTransaction(@RequestBody BulkTransactionRequest request) {
      return this.bulkAccountConfigService.createUploadBulkTransaction(request);
    }

    @GetMapping()
    public List<BulkTransactionResponse> getAllBulkTransaction() {
       return this.bulkAccountConfigService.getAllBulk();
    }

    @GetMapping("/{id}")
    public BulkTransactionResponse getByIdBulkTransaction(@PathVariable("id") Long id)  {
        return this.bulkAccountConfigService.getByIdBulkTransaction(id);
    }

    @PutMapping("/{id}")
    public BulkTransactionResponse updateBulkTransaction(@PathVariable("id") Long id, @Validated @RequestBody BulkTransactionRequest request) {
        return bulkAccountConfigService.updateBulkTransaction(id, request);
    }


    @PostMapping("transactions")
    public UploadBulkTransactionResponse saveTransacions(@RequestBody UploadBulkTransactionResponse request) {
        return this.bulkAccountConfigService.saveTransactionsUpload(request);
    }

}
