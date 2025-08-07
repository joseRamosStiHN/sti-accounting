package com.sti.accounting.services;


import com.sti.accounting.entities.*;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.*;
import com.sti.accounting.utils.BulkDetailType;
import com.sti.accounting.utils.Currency;
import com.sti.accounting.utils.Motion;
import com.sti.accounting.utils.Status;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BulkAccountConfigService {

    private final IBulkAccountConfigRepository bulkAccountConfigRepository;
    private final ITransactionRepository transactionRepository;
    private final IDocumentRepository document;
    private final IAccountRepository iAccountRepository;
    private final IAccountingJournalRepository accountingJournalRepository;
    private final AccountingPeriodService accountingPeriodService;
    private final AuthService authService;


    public BulkAccountConfigService(IBulkAccountConfigRepository bulkAccountConfigRepository, ITransactionRepository transactionRepository, IDocumentRepository document,
                                    IAccountRepository accountRepository, IAccountingJournalRepository iAccountingJournalRepository,
                                    AccountingPeriodService accountingPeriodService, AuthService authService) {
        this.bulkAccountConfigRepository = bulkAccountConfigRepository;
        this.transactionRepository = transactionRepository;
        this.document = document;
        this.iAccountRepository = accountRepository;
        this.accountingJournalRepository = iAccountingJournalRepository;
        this.accountingPeriodService = accountingPeriodService;
        this.authService = authService;
    }

    public UploadBulkTransactionResponse excelToObject(MultipartFile file, Long id) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request");

        BulkAccountConfig config = bulkAccountConfigRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        AccountingPeriodEntity activePeriod = getActivePeriodOrThrow();
        int startRow = config.getRowStart() - 1;

        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            UploadBulkTransactionResponse response = new UploadBulkTransactionResponse();
            response.setTypeTransaction(config.getType());

            List<UploadBulkTransaction> data = new ArrayList<>();
            List<UploadBulkTransaction> errors = new ArrayList<>();
            Set<String> references = new HashSet<>();

            for (int i = startRow; i < sheet.getLastRowNum(); i++) {
                UploadBulkTransaction records = new UploadBulkTransaction();
                records.setRow(i + 1);
                boolean hasError = processRow(sheet.getRow(i), config, evaluator, records, activePeriod, references);

                if (!validateBalance(records)) hasError = true;

                records.setStatus(Status.DRAFT);
                (hasError ? errors : data).add(records);
            }

            response.setData(data);
            response.setErrors(errors);
            return response;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private AccountingPeriodEntity getActivePeriodOrThrow() {
        try {
            return accountingPeriodService.getActivePeriod();
        } catch (ResponseStatusException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay un periodo contable activo");
        }
    }

    private boolean processRow(Row row, BulkAccountConfig config, FormulaEvaluator evaluator,
                               UploadBulkTransaction transaction, AccountingPeriodEntity activePeriod,
                               Set<String> references) {
        boolean error = false;

        for (BulkAccountConfigDetail detail : config.getDetails()) {
            String strValue = getCellValue(row.getCell(detail.getColIndex()), evaluator);
            error |= processField(detail, strValue, transaction, activePeriod, references);
            if ("DETALLE".equalsIgnoreCase(detail.getTitle()) && "NULO".equalsIgnoreCase(strValue)) return true;
        }

        if (transaction.getAccounts() == null || transaction.getAccounts().isEmpty()) {
            transaction.setErrors(errorMessage(transaction.getErrors(), "No se encontraron cuentas para la partida"));
            return true;
        }

        return error;
    }

    private String getCellValue(Cell cell, FormulaEvaluator evaluator) {
        DataFormatter formatter = new DataFormatter();
        if (cell == null) return "";
        return cell.getCellType() == CellType.FORMULA
                ? String.valueOf(evaluator.evaluate(cell).getNumberValue())
                : formatter.formatCellValue(cell);
    }

    private boolean processField(BulkAccountConfigDetail detail, String strValue,
                                 UploadBulkTransaction transaction, AccountingPeriodEntity activePeriod,
                                 Set<String> references) {
        String field = detail.getTitle();
        boolean error = false;
        switch (field) {
            case "CURRENCY":
                transaction.setCurrency((strValue == null || strValue.trim().isEmpty()) ? "L" : strValue.trim().toUpperCase());
                break;
            case "FECHA":
                error |= processDateField(strValue, transaction, activePeriod);
                break;
            case "FACTURA":
                error |= processReferenceField(strValue, transaction, references);
                break;
            case "CON-RTN": transaction.setRtn(strValue); break;
            case "SUPPLIER_value": transaction.setSupplierName(strValue); break;

            case "TIPO-VENTA":
                transaction.setTypeSale((strValue == null || strValue.trim().isEmpty()) ? "CONTADO" : strValue.trim()); break;
            case "TIPO-PAGO": transaction.setTypePayment(strValue); break;
            case "DETALLE": transaction.setDescription(strValue); break;
            case "TIPO_CAMBIO":
                transaction.setExchangeRate(isValidNumber(strValue) ? new BigDecimal(strValue).setScale(4, RoundingMode.HALF_UP) : new BigDecimal("24.70"));
                break;
            default:
                if (BulkDetailType.ACC.equals(detail.getDetailType())) {
                    addAccountField(detail, strValue, transaction);
                } else {
                    addOtherField(detail, strValue, transaction);
                }
        }
        return error;
    }

    private boolean processDateField(String strValue, UploadBulkTransaction transaction, AccountingPeriodEntity period) {
        if (!isValidDate(strValue)) {
            transaction.setErrors(errorMessage(transaction.getErrors(), "Fecha '" + strValue + "' con formato incorrecto"));
            return true;
        }

        try {
            LocalDate date = LocalDate.parse(strValue, DateTimeFormatter.ofPattern("M/d/yyyy"));
            if (date.isBefore(period.getStartPeriod().toLocalDate())) {
                transaction.setErrors(errorMessage(transaction.getErrors(), "La fecha '" + strValue + "' proporcionada es anterior al periodo contable activo."));
                return true;
            } else if (period.getEndPeriod() != null && date.isAfter(period.getEndPeriod().toLocalDate())) {
                transaction.setErrors(errorMessage(transaction.getErrors(), "La fecha '" + strValue + "' proporcionada no corresponde al periodo contable activo."));
                return true;
            }
            transaction.setDate(strValue);
        } catch (Exception e) {
            transaction.setErrors(errorMessage(transaction.getErrors(), "Fecha '" + strValue + "' con formato incorrecto"));
            return true;
        }
        return false;
    }

    private boolean processReferenceField(String strValue, UploadBulkTransaction transaction, Set<String> references) {
        if (strValue == null || strValue.isEmpty()) {
            transaction.setReference(strValue);
            return false;
        }

        if (references.contains(strValue)) {
            transaction.setErrors(errorMessage(transaction.getErrors(), "La factura ingresada '" + strValue + "' ya existe en el sistema."));
            return true;
        }

        if (transactionRepository.existsByReferenceAndTenantId(strValue, authService.getTenantId())) {
            transaction.setErrors(errorMessage(transaction.getErrors(), "La factura ingresada '" + strValue + "' ya existe en el sistema."));
            return true;
        }

        references.add(strValue);
        transaction.setReference(strValue);
        return false;
    }


    private void addAccountField(BulkAccountConfigDetail detail, String strValue, UploadBulkTransaction transaction) {
        UploadBulkAccountsListResponse account = getAccountsListResponse(detail, strValue);
        transaction.getAccounts().add(account);
    }

    private void addOtherField(BulkAccountConfigDetail detail, String strValue, UploadBulkTransaction transaction) {
        UploadBulkOthersFieldsList field = getAnotherFields(detail.getTitle(), strValue);
        transaction.getOtherFields().add(field);
    }

    private boolean validateBalance(UploadBulkTransaction transaction) {
        if (transaction.getAccounts() == null || transaction.getAccounts().isEmpty()) return false;

        BigDecimal debit = transaction.getAccounts().stream().map(UploadBulkAccountsListResponse::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = transaction.getAccounts().stream().map(UploadBulkAccountsListResponse::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (debit.compareTo(credit) != 0) {
            transaction.setErrors(errorMessage(transaction.getErrors(), "Los valores de débito y crédito no están balanceados."));
            return false;
        }
        return true;
    }

    private static UploadBulkAccountsListResponse getAccountsListResponse(BulkAccountConfigDetail detail, String strValue) {
        UploadBulkAccountsListResponse uploadAccounts = new UploadBulkAccountsListResponse();

        uploadAccounts.setTitle(detail.getTitle());
        uploadAccounts.setAccount(detail.getAccountId());
        if (detail.getOperation().equalsIgnoreCase("D") && isNumeric(strValue)) {
            uploadAccounts.setDebit(new BigDecimal(strValue).setScale(2, RoundingMode.HALF_UP));
            uploadAccounts.setCredit(BigDecimal.ZERO);
        } else if (detail.getOperation().equalsIgnoreCase("C") && isNumeric(strValue)) {
            uploadAccounts.setCredit(new BigDecimal(strValue).setScale(2, RoundingMode.HALF_UP));
            uploadAccounts.setDebit(BigDecimal.ZERO);
        } else {
            uploadAccounts.setCredit(BigDecimal.ZERO);
            uploadAccounts.setDebit(BigDecimal.ZERO);
        }
        return uploadAccounts;
    }

    private static UploadBulkOthersFieldsList getAnotherFields(String header, String strValue) {
        UploadBulkOthersFieldsList uploadBulkOthersFieldsList = new UploadBulkOthersFieldsList();
        uploadBulkOthersFieldsList.setKey(header);
        uploadBulkOthersFieldsList.setValue(strValue);
        return uploadBulkOthersFieldsList;
    }

    public List<BulkTransactionResponse> getAllBulk() {

        List<BulkAccountConfig> bulkAccountConfig = this.bulkAccountConfigRepository.findByTenantId(authService.getTenantId());
        return getAllBulkAccountsConfigResponse(bulkAccountConfig);
    }

    public BulkTransactionResponse getByIdBulkTransaction(Long id) {

        BulkAccountConfig bulkAccountConfig = this.bulkAccountConfigRepository.findByIdAndTenantId(id, authService.getTenantId());

        if (bulkAccountConfig == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No exist bulk transaction");
        }

        return getBulkAccountsConfigResponse(bulkAccountConfig);
    }


    public BulkTransactionResponse updateBulkTransaction(Long id, BulkTransactionRequest request) {

        this.bulkAccountConfigRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No exist bulk transaction"));

        BulkAccountConfig bulkAccountConfig = createBulkAccountConfig(request);
        bulkAccountConfig.setId(id);

        BulkAccountConfig accountConfig = bulkAccountConfigRepository.save(bulkAccountConfig);

        return getBulkAccountsConfigResponse(accountConfig);
    }


    public BulkTransactionResponse createUploadBulkTransaction(BulkTransactionRequest request) {

        BulkAccountConfig bulkAccountConfigExist = bulkAccountConfigRepository.findByName(request.getName());

        if (bulkAccountConfigExist != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Config already exist");
        }

        try {
            BulkAccountConfig bulkAccountConfigRequest = this.createBulkAccountConfig(request);

            BulkAccountConfig bulkAccountConfig = bulkAccountConfigRepository.save(bulkAccountConfigRequest);

            return getBulkAccountsConfigResponse(bulkAccountConfig);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public UploadBulkTransactionResponse saveTransactionsUpload(UploadBulkTransactionResponse request) {
        try {
            List<UploadBulkTransaction> merged = mergeRequests(request);
            DocumentEntity documentType = getDocumentEntity(request.getTypeTransaction());
            AccountingJournalEntity accountingJournal = getAccountingJournal(request.getTypeTransaction());
            AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

            UploadBulkTransactionResponse response = new UploadBulkTransactionResponse();
            response.setTypeTransaction(request.getTypeTransaction());

            List<TransactionEntity> entitiesToSave = new ArrayList<>();
            List<UploadBulkTransaction> successTransactions = new ArrayList<>();
            List<UploadBulkTransaction> errorTransactions = new ArrayList<>();

            for (UploadBulkTransaction transaction : merged) {
                TransactionEntity entity = createTransactionEntity(transaction, documentType, accountingJournal, activePeriod);

                if (validateTransaction(entity, transaction)) {
                    entitiesToSave.add(entity);
                    successTransactions.add(transaction);
                } else {
                    errorTransactions.add(transaction);
                }
            }

            transactionRepository.saveAll(entitiesToSave);

            response.setData(successTransactions);
            response.setErrors(errorTransactions);
            return response;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private List<UploadBulkTransaction> mergeRequests(UploadBulkTransactionResponse request) {
        List<UploadBulkTransaction> merged = new ArrayList<>();
        merged.addAll(request.getData());
        merged.addAll(request.getErrors());
        return merged;
    }

    private DocumentEntity getDocumentEntity(Long typeId) {
        return document.findById(typeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Document type %d not valid", typeId)));
    }

    private AccountingJournalEntity getAccountingJournal(Long typeId) {
        return accountingJournalRepository.findById(typeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Diary type %d not valid", typeId)));
    }

    private TransactionEntity createTransactionEntity(UploadBulkTransaction transaction,
                                                      DocumentEntity documentType, AccountingJournalEntity accountingJournal,
                                                      AccountingPeriodEntity activePeriod) {
        TransactionEntity entity = new TransactionEntity();
        entity.setDocument(documentType);
        entity.setStatus(StatusTransaction.DRAFT);
        entity.setCurrency("USD".equalsIgnoreCase(transaction.getCurrency()) ? Currency.USD : Currency.L);
        entity.setExchangeRate(new BigDecimal("24.70"));
        entity.setReference(transaction.getReference());
        entity.setDescriptionPda(transaction.getDescription());
        entity.setAccountingJournal(accountingJournal);
        entity.setAccountingPeriod(activePeriod);
        entity.setCreateAtDate(parseTransactionDate(transaction.getDate()));
        entity.setTypeSale(transaction.getTypeSale() != null ? transaction.getTypeSale() : "CONTADO");
        entity.setTypePayment(transaction.getTypePayment());
        entity.setRtn(transaction.getRtn() != null ? transaction.getRtn().toUpperCase() : null);
        entity.setSupplierName(transaction.getSupplierName());
        entity.setTenantId(authService.getTenantId());

        List<TransactionDetailEntity> details = detailToEntity(entity, transaction.getAccounts());
        entity.setTransactionDetail(details);

        return entity;
    }

    private LocalDate parseTransactionDate(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean validateTransaction(TransactionEntity entity, UploadBulkTransaction transaction) {
        boolean isValid = true;

        if (entity.getReference() == null || entity.getReference().isEmpty()) {
            addError(transaction, "Factura es obligatoria");
            isValid = false;
        }

        if (entity.getDescriptionPda() == null || entity.getDescriptionPda().isEmpty()) {
            addError(transaction, "Descripción  es obligatoria");
            isValid = false;
        }

        if (entity.getCreateAtDate() == null) {
            addError(transaction, "Fecha es obligatoria");
            isValid = false;
        }

        if (entity.getRtn() == null || entity.getRtn().isEmpty()) {
            addError(transaction, "RTN es obligatorio");
            isValid = false;
        }

        BigDecimal totalDebit = entity.getTransactionDetail().stream()
                .filter(d -> Motion.D.equals(d.getMotion()))
                .map(TransactionDetailEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = entity.getTransactionDetail().stream()
                .filter(d -> Motion.C.equals(d.getMotion()))
                .map(TransactionDetailEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.compareTo(totalCredit) != 0) {
            addError(transaction, "Los valores de débito y crédito no están balanceados.");
            isValid = false;
        }

        return isValid;
    }

    private void addError(UploadBulkTransaction transaction, String message) {
        String errorMessage = errorMessage(transaction.getErrors(),
                "Fila: " + transaction.getRow() + " " + message);
        transaction.setErrors(errorMessage);
    }


    private List<TransactionDetailEntity> detailToEntity(TransactionEntity transactionEntity, List<UploadBulkAccountsListResponse> detailRequests) {
        try {
            List<TransactionDetailEntity> result = new ArrayList<>();
            List<AccountEntity> accounts = iAccountRepository.findAll();
            for (UploadBulkAccountsListResponse detail : detailRequests) {
                TransactionDetailEntity entity = new TransactionDetailEntity();
                Optional<AccountEntity> currentAccount = accounts.stream().filter(x -> x.getId().equals(detail.getAccount())).findFirst();
                if (currentAccount.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The account with id " + detail.getAccount() + "does not exist.");
                }
                currentAccount.ifPresent(entity::setAccount);

                if (detail.getCredit().compareTo(BigDecimal.ZERO) != 0) {
                    entity.setAmount(detail.getCredit());
                    entity.setMotion(Motion.C);
                    entity.setTransaction(transactionEntity);
                    result.add(entity);
                } else if (detail.getDebit().compareTo(BigDecimal.ZERO) != 0) {
                    entity.setAmount(detail.getDebit());
                    entity.setMotion(Motion.D);
                    entity.setTransaction(transactionEntity);
                    result.add(entity);
                }

            }
            return result;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account or accounts in detail do not exist");
        }
    }


    private static BulkAccountConfigDetail getBulkAccountConfigDetail(BulkTransactionRequestList details, BulkAccountConfig bulkAccountConfigRequest) {
        BulkAccountConfigDetail bulkAccountConfigDetail = new BulkAccountConfigDetail();
        bulkAccountConfigDetail.setAccountId(details.getAccount());
        bulkAccountConfigDetail.setColIndex(details.getColum());
        bulkAccountConfigDetail.setDetailType(details.getBulkTypeData());
        bulkAccountConfigDetail.setTitle(details.getTitle());
        bulkAccountConfigDetail.setOperation(details.getOperation());
        bulkAccountConfigDetail.setBulkAccountConfig(bulkAccountConfigRequest);
        return bulkAccountConfigDetail;
    }

    public BulkAccountConfig createBulkAccountConfig(BulkTransactionRequest request) {

        BulkAccountConfig bulkAccountConfigRequest = new BulkAccountConfig();
        List<BulkAccountConfigDetail> bulkAccountConfigDetailList = new ArrayList<>();
        for (BulkTransactionRequestList details : request.getConfigDetails()) {
            BulkAccountConfigDetail bulkAccountConfigDetail = getBulkAccountConfigDetail(details, bulkAccountConfigRequest);
            bulkAccountConfigDetailList.add(bulkAccountConfigDetail);
        }
        bulkAccountConfigRequest.setDetails(bulkAccountConfigDetailList);

        bulkAccountConfigRequest.setIsActive(true);
        bulkAccountConfigRequest.setName(request.getName());
        bulkAccountConfigRequest.setRowStart(request.getRowInit());
        bulkAccountConfigRequest.setTenantId(this.authService.getTenantId());
        bulkAccountConfigRequest.setType(request.getType());

        return bulkAccountConfigRequest;
    }


    private static List<BulkTransactionResponse> getAllBulkAccountsConfigResponse(List<BulkAccountConfig> bulkAccountConfig) {

        List<BulkTransactionResponse> result = new ArrayList<>();

        for (BulkAccountConfig bulkAccount : bulkAccountConfig) {

            BulkTransactionResponse uploadBulkTransactionResponse = new BulkTransactionResponse();
            uploadBulkTransactionResponse.setId(bulkAccount.getId());
            uploadBulkTransactionResponse.setStatus(bulkAccount.getIsActive());
            uploadBulkTransactionResponse.setName(bulkAccount.getName());
            uploadBulkTransactionResponse.setRowInit(bulkAccount.getRowStart());
            uploadBulkTransactionResponse.setTenantId(bulkAccount.getTenantId());
            uploadBulkTransactionResponse.setType(bulkAccount.getType());

            List<BulkTransactionRequestList> uploadBulkTransactionResponseList = new ArrayList<>();

            for (BulkAccountConfigDetail bulkAccountConfigDetail : bulkAccount.getDetails()) {
                BulkTransactionRequestList uploadBulkTransactionRequestList = getUploadBulkTransactionRequestList(bulkAccountConfigDetail);
                uploadBulkTransactionResponseList.add(uploadBulkTransactionRequestList);
            }

            uploadBulkTransactionResponse.setConfigDetails(uploadBulkTransactionResponseList);
            result.add(uploadBulkTransactionResponse);
        }


        return result;
    }


    private static BulkTransactionResponse getBulkAccountsConfigResponse(BulkAccountConfig bulkAccount) {

        BulkTransactionResponse uploadBulkTransactionResponse = new BulkTransactionResponse();
        uploadBulkTransactionResponse.setId(bulkAccount.getId());
        uploadBulkTransactionResponse.setStatus(bulkAccount.getIsActive());
        uploadBulkTransactionResponse.setName(bulkAccount.getName());
        uploadBulkTransactionResponse.setRowInit(bulkAccount.getRowStart());
        uploadBulkTransactionResponse.setTenantId(bulkAccount.getTenantId());
        uploadBulkTransactionResponse.setType(bulkAccount.getType());

        List<BulkTransactionRequestList> uploadBulkTransactionResponseList = new ArrayList<>();

        for (BulkAccountConfigDetail bulkAccountConfigDetail : bulkAccount.getDetails()) {
            BulkTransactionRequestList uploadBulkTransactionRequestList = getUploadBulkTransactionRequestList(bulkAccountConfigDetail);
            uploadBulkTransactionResponseList.add(uploadBulkTransactionRequestList);
        }
        uploadBulkTransactionResponse.setConfigDetails(uploadBulkTransactionResponseList);
        return uploadBulkTransactionResponse;
    }

    private static BulkTransactionRequestList getUploadBulkTransactionRequestList(BulkAccountConfigDetail bulkAccountConfigDetail) {
        BulkTransactionRequestList uploadBulkTransactionRequestList = new BulkTransactionRequestList();
        uploadBulkTransactionRequestList.setAccount(bulkAccountConfigDetail.getAccountId());
        uploadBulkTransactionRequestList.setColum(bulkAccountConfigDetail.getColIndex());
        uploadBulkTransactionRequestList.setBulkTypeData(bulkAccountConfigDetail.getDetailType());
        uploadBulkTransactionRequestList.setTitle(bulkAccountConfigDetail.getTitle());
        uploadBulkTransactionRequestList.setOperation(bulkAccountConfigDetail.getOperation());

        return uploadBulkTransactionRequestList;
    }

    public static boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            new BigDecimal(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidNumber(String cadena) {
        if (cadena == null || cadena.isEmpty()) {
            return false;
        }
        String patron = "^[+-]?\\d+(\\.\\d+)?$";
        return cadena.matches(patron);
    }

    public static boolean isValidDate(String fechaStr) {
        if (fechaStr == null || fechaStr.isEmpty()) {
            return false;
        }
        String[] formatos = {"M/d/yyyy", "MM/dd/yyyy", "d/M/yyyy", "dd/MM/yyyy"};

        for (String formato : formatos) {
            if (esFechaConFormato(fechaStr, formato)) {
                return true;
            }
        }
        return false;
    }

    private static boolean esFechaConFormato(String fechaStr, String formato) {
        SimpleDateFormat sdf = new SimpleDateFormat(formato);
        sdf.setLenient(false);

        try {
            Date fecha = sdf.parse(fechaStr);
            return fechaStr.equals(sdf.format(fecha));
        } catch (Exception e) {
            return false;
        }
    }

    public static String errorMessage(String description, String message) {
        return description == null ||
                description.isEmpty()
                ? message
                : description + "," + message;

    }


}
