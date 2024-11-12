package com.sti.accounting.controllers;

import com.sti.accounting.services.BulkAccountConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/upload-bulk-transaction")
public class UploadBulkTransactionController {
    private static final Logger logger = LoggerFactory.getLogger(UploadBulkTransactionController.class);
    private final BulkAccountConfigService bulkAccountConfigService;

    public UploadBulkTransactionController(BulkAccountConfigService bulkAccountConfigService) {
        this.bulkAccountConfigService = bulkAccountConfigService;
    }

    @PostMapping
    public void uploadBulkTransaction(@RequestPart("file") MultipartFile file) {
        this.bulkAccountConfigService.ExcelToObject(file);
    }
}
