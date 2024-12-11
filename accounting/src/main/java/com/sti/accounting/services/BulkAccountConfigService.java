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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.swing.text.html.parser.Entity;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class BulkAccountConfigService {

    private static final Logger logger = LoggerFactory.getLogger(BulkAccountConfigService.class);

    private final IBulkAccountConfigRepository bulkAccountConfigRepository;
    private final ITransactionRepository transactionRepository;
    private final IDocumentRepository document;
    private final IAccountRepository iAccountRepository;
    private final IAccountingJournalRepository accountingJournalRepository;


    public BulkAccountConfigService(IBulkAccountConfigRepository bulkAccountConfigRepository, ITransactionRepository transactionRepository,IDocumentRepository document,
                                    IAccountRepository accountRepository, IAccountingJournalRepository iAccountingJournalRepository) {
        this.bulkAccountConfigRepository = bulkAccountConfigRepository;
        this.transactionRepository = transactionRepository;
        this.document = document;
        this.iAccountRepository= accountRepository;
        this.accountingJournalRepository = iAccountingJournalRepository;
    }

    public UploadBulkTransactionResponse  ExcelToObject(MultipartFile file, Long id) {


        if(id == null ){
           throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request");
        }
        // get configuration        /// Id y tenant ID
        BulkAccountConfig configs = this.bulkAccountConfigRepository.findById(id)
                                            .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));

        //start row
        int startRow = configs.getRowStart() - 1;

        try(InputStream inputStream = file.getInputStream()){
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0); // get first book
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            UploadBulkTransactionResponse uploadBulkTransactionResponse = new UploadBulkTransactionResponse();
            uploadBulkTransactionResponse.setTypetransaction(configs.getType());

            List<UploadBulkTransaction> data = new ArrayList<>();
            List<UploadBulkTransaction> errors = new ArrayList<>();

            outerLoop:
            for (int i = startRow; i < sheet.getLastRowNum(); i++) {
                //get cell value
                boolean error = false;
                UploadBulkTransaction uploadBulkTransactionData = new UploadBulkTransaction();

                uploadBulkTransactionData.setRow(i +1 );
                for (BulkAccountConfigDetail detail : configs.getDetails()) {
                    DataFormatter formatter = new DataFormatter();

                        CellType cellType = sheet.getRow(i).getCell(detail.getColIndex()).getCellType();
                        String strValue = "";
                        if (cellType == CellType.FORMULA) {
                            strValue = String.valueOf(evaluator.evaluate(sheet.getRow(i).getCell(detail.getColIndex())).getNumberValue());
                        } else {
                            strValue = formatter.formatCellValue(sheet.getRow(i).getCell(detail.getColIndex()));
                        }
                            String field = detail.getTitle();
                            switch (field) {
                                case "CURRENCY":
                                    uploadBulkTransactionData.setCurrency(strValue);
                                    break;
                                case "FECHA":
                                    if (esFechaValida(strValue)){
                                        uploadBulkTransactionData.setDate(strValue);
                                    }else{
                                        String errorMessage = errorMessage(uploadBulkTransactionData.getErrors(),"Fecha con formato incorrecto ");
                                        uploadBulkTransactionData.setErrors(errorMessage);
                                        error = true;
                                    }
                                    break;
                                case "DETALLE":

                                    if (strValue.equalsIgnoreCase("NULO")){
                                        continue outerLoop;
                                    }
                                    uploadBulkTransactionData.setDescription(strValue);
                                    break;
                                case "TIPO_CAMBIO":
                                    if (esNumeroValido(strValue)){
                                        uploadBulkTransactionData.setExchangeRate(new BigDecimal(strValue).setScale(4, RoundingMode.HALF_UP));
                                    }else{
                                        String errorMessage = errorMessage(uploadBulkTransactionData.getErrors(),"Tipo de Cambio debería ser numérico");
                                        uploadBulkTransactionData.setErrors(errorMessage);
                                        error = true;
                                    }
                                    break;
                                case "FACTURA":
                                    uploadBulkTransactionData.setReference(strValue);
                                    break;
                                case "CON-RTN":
                                    uploadBulkTransactionData.setRtn(strValue);
                                    break;
                                case "SUPPLIER_value":
                                    uploadBulkTransactionData.setSupplierName(strValue);
                                    break;
                                case "TIPO-VENTA":

                                    uploadBulkTransactionData.setTypePayment(strValue);
                                    break;
                                case "TIPO-PAGO":
                                    uploadBulkTransactionData.setTypeSale(strValue);
                                    break;
                                default:
                                    if ( BulkDetailType.ACC.equals(detail.getDetailType())){

                                        UploadBulkAccountsListResponse uploadAccounts = getAccountsListResponse(detail, strValue);
                                        List<UploadBulkAccountsListResponse> accountsList = uploadBulkTransactionData.getAccounts();
                                        if (accountsList == null || accountsList.isEmpty()) {
                                            accountsList = new ArrayList<>();
                                        }
                                        accountsList.add(uploadAccounts);
                                        uploadBulkTransactionData.setAccounts(accountsList);

                                    }else {
                                        UploadBulkOthersFieldsList uploadOthersFields = getAnotherFields(detail.getTitle(), strValue);
                                        List<UploadBulkOthersFieldsList> uploadOthersFieldsList = uploadBulkTransactionData.getOtherFields();
                                        if (uploadOthersFieldsList == null || uploadOthersFieldsList.isEmpty()) {
                                            uploadOthersFieldsList = new ArrayList<>();
                                        }
                                        uploadOthersFieldsList.add(uploadOthersFields);
                                        uploadBulkTransactionData.setOtherFields(uploadOthersFieldsList);
                                    }
                            }
                }


                if (uploadBulkTransactionData.getAccounts()!=null){
                    BigDecimal totalDebit = BigDecimal.ZERO;
                    BigDecimal totalCredit = BigDecimal.ZERO;

                    for (UploadBulkAccountsListResponse account: uploadBulkTransactionData.getAccounts()){
                        totalDebit = totalDebit.add(account.getDebit());
                        totalCredit = totalCredit.add(account.getCredit());
                    }
                    if (totalDebit.compareTo(totalCredit) != 0) {

                        String errorMessage = errorMessage(uploadBulkTransactionData.getErrors(),"Partida No cuadra");
                        uploadBulkTransactionData.setErrors(errorMessage);
                        error = true;
                    }

                }else{
                    String errorMessage = errorMessage(uploadBulkTransactionData.getErrors(),"No se encontraron cuentas para la partida");
                    uploadBulkTransactionData.setErrors(errorMessage);
                    error = true;
                }

                uploadBulkTransactionData.setStatus(Status.DRAFT);

                if (error){
                    errors.add(uploadBulkTransactionData);
                }else{
                    data.add(uploadBulkTransactionData);
                }

                uploadBulkTransactionResponse.setData(data);
                uploadBulkTransactionResponse.setErrors(errors);

            }


            return  uploadBulkTransactionResponse;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static UploadBulkAccountsListResponse getAccountsListResponse(BulkAccountConfigDetail detail, String strValue) {
        UploadBulkAccountsListResponse uploadAccounts = new UploadBulkAccountsListResponse();

        uploadAccounts.setTitle(detail.getTitle());
        uploadAccounts.setAccount(detail.getAccountId());
        if (detail.getOperation().equalsIgnoreCase("D") && isNumeric(strValue)){
            uploadAccounts.setDebit(new BigDecimal(strValue).setScale(2, RoundingMode.HALF_UP));
            uploadAccounts.setCredit(BigDecimal.ZERO);
        }else if (detail.getOperation().equalsIgnoreCase("C") && isNumeric(strValue) ){
            uploadAccounts.setCredit(new BigDecimal(strValue).setScale(2, RoundingMode.HALF_UP));
            uploadAccounts.setDebit(BigDecimal.ZERO);
        }else{
            uploadAccounts.setCredit(BigDecimal.ZERO);
            uploadAccounts.setDebit(BigDecimal.ZERO);
        }
        return uploadAccounts;
    }

    private static UploadBulkOthersFieldsList getAnotherFields( String header, String strValue){
        UploadBulkOthersFieldsList uploadBulkOthersFieldsList = new UploadBulkOthersFieldsList();
        uploadBulkOthersFieldsList.setKey(header);
        uploadBulkOthersFieldsList.setValue(strValue);
        return uploadBulkOthersFieldsList;
    }

    public List<BulkTransactionResponse> getAllBulk(){

        List<BulkAccountConfig> bulkAccountConfig =  this.bulkAccountConfigRepository.findAll();

        return   getAllBulkAccountsConfigResponse(bulkAccountConfig);
    }

    public BulkTransactionResponse getByIdBulkTransaction(Long id){

        BulkAccountConfig bulkAccountConfig =  this.bulkAccountConfigRepository.findById(id)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"No exist bulk transaction"));

        return getBulkAccountsConfigResponse(bulkAccountConfig);
    }


    public BulkTransactionResponse updateBulkTransaction(Long id, BulkTransactionRequest request){

         this.bulkAccountConfigRepository.findById(id).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND,"No exist bulk transaction"));

        BulkAccountConfig bulkAccountConfig = createBulkAccountConfig(request);
        bulkAccountConfig.setId(id);

        BulkAccountConfig accountConfig =  bulkAccountConfigRepository.save(bulkAccountConfig);

        return  getBulkAccountsConfigResponse(accountConfig);
    }



    public BulkTransactionResponse createUploadBulkTransaction(BulkTransactionRequest request){

        BulkAccountConfig bulkAccountConfigExist = bulkAccountConfigRepository.findByName(request.getName());

        if (bulkAccountConfigExist != null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Config already exist");
        }

        try{
            BulkAccountConfig bulkAccountConfigRequest =this.createBulkAccountConfig(request);

            BulkAccountConfig bulkAccountConfig = bulkAccountConfigRepository.save(bulkAccountConfigRequest);

            return getBulkAccountsConfigResponse(bulkAccountConfig);

        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,e.getMessage());
        }
    }

    public UploadBulkTransactionResponse saveTransacionsUpload(UploadBulkTransactionResponse request){


        try{

            List<UploadBulkTransaction> merged = new ArrayList<>();
            merged.addAll(request.getData());
            merged.addAll(request.getErrors());

            List<TransactionEntity>  entityList= new ArrayList<>();
            // Get Document
            DocumentEntity documentType = document.findById(request.getTypetransaction())
                    .orElseThrow(
                            () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    String.format("Document type %d not valid ", request.getTypetransaction())
                            )
                    );

            AccountingJournalEntity accountingJournal = accountingJournalRepository.findById(request.getTypetransaction()).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            String.format("Diary type %d not valid ", request.getTypetransaction())
                    )
            );

                    UploadBulkTransactionResponse  uploadBulkTransactionResponse = new UploadBulkTransactionResponse();
            uploadBulkTransactionResponse.setTypetransaction(request.getTypetransaction());
            List<UploadBulkTransaction>  uploadBulkTransactionsError = new ArrayList<>();
            List<UploadBulkTransaction>  uploadBulkTransactionsData = new ArrayList<>();
            for (UploadBulkTransaction transaction: merged){


                transaction.setErrors(null);

                boolean save = true;

                TransactionEntity entity = new TransactionEntity();

                entity.setDocument(documentType);
                entity.setStatus(StatusTransaction.DRAFT);
                if (transaction.getCurrency() != null && transaction.getCurrency().equalsIgnoreCase("L")){
                    entity.setCurrency(Currency.L);
                }else  if (transaction.getCurrency() != null && transaction.getCurrency().equalsIgnoreCase("USD")){
                    entity.setCurrency(Currency.USD);
                }else{
                    String errorMessage = errorMessage(transaction.getErrors(),"Fila: "+transaction.getRow()+" Tipo de moneda no existe guardado como lempiras");
                    transaction.setErrors(errorMessage);
                    entity.setCurrency(Currency.L);
                }

                entity.setExchangeRate(transaction.getExchangeRate());

                if (transaction.getReference() == null || transaction.getReference().isEmpty()){
                    save = false;
                    String errorMessage = errorMessage(transaction.getErrors(),"Fila: "+transaction.getRow()+" Factura es obligatoria");
                    transaction.setErrors(errorMessage);
                }else {
                    entity.setReference(transaction.getReference());
                }

                if (transaction.getDescription() == null || transaction.getDescription().isEmpty()){
                    save = false;
                    String errorMessage = errorMessage(transaction.getErrors(),"Fila: "+transaction.getRow()+" Descripcion es obligatoria");
                    transaction.setErrors(errorMessage);
                }else {
                    entity.setDescriptionPda(transaction.getDescription());
                }
                
               entity.setAccountingJournal(accountingJournal);

                try{
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
                    LocalDate localDate = LocalDate.parse(transaction.getDate(), formatter);
                    entity.setCreateAtDate(localDate);
                }catch (Exception e){
                    String errorMessage = errorMessage(transaction.getErrors(),"Fila: "+transaction.getRow()+" Fecha es obligatoria");
                    transaction.setErrors(errorMessage);
                    entity.setCreateAtDate(null);
                    save = false;
                }

                if (transaction.getTypeSale() == null || transaction.getTypeSale().isEmpty()){
                    save = false;
                    String errorMessage = errorMessage(transaction.getErrors(),"Fila: "+transaction.getRow()+" Typo de venta es obligatoria");
                    transaction.setErrors(errorMessage);
                }else {
                    entity.setTypeSale(transaction.getTypeSale());
                }

                entity.setTypePayment(transaction.getTypePayment());

                if(transaction.getRtn() == null || transaction.getRtn().isEmpty()){

                    String errorMessage = errorMessage(transaction.getErrors(),"Fila: "+transaction.getRow()+" Rtn es obligatorio");
                    transaction.setErrors(errorMessage);
                    save = false;
                }else {
                    entity.setRtn(transaction.getRtn());
                }

                entity.setSupplierName(transaction.getSupplierName());
                List<TransactionDetailEntity> transactionDetailEntities = detailToEntity(entity, transaction.getAccounts());

                BigDecimal totalDebit = transactionDetailEntities.stream()
                        .filter(detail -> Motion.D.equals(detail.getMotion()))
                        .map(TransactionDetailEntity::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalCredit= transactionDetailEntities.stream()
                        .filter(detail -> Motion.C.equals(detail.getMotion()))
                        .map(TransactionDetailEntity::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);


                if (totalDebit.compareTo(totalCredit) !=0){


                    String errorMessage = errorMessage(transaction.getErrors(),"Fila: "+transaction.getRow()+" Movimientos no esta cuadrados correctamente");
                    transaction.setErrors(errorMessage);
                    save = false;
                }

                entity.setTransactionDetail(transactionDetailEntities);

                if (save){
                    entityList.add(entity);
                    uploadBulkTransactionsData.add(transaction);

                }else{
                    uploadBulkTransactionsError.add(transaction);
                }

            }

            uploadBulkTransactionResponse.setTypetransaction(request.getTypetransaction());
            uploadBulkTransactionResponse.setData(uploadBulkTransactionsData);
            uploadBulkTransactionResponse.setErrors(uploadBulkTransactionsError);


            transactionRepository.saveAll(entityList);


            return uploadBulkTransactionResponse;

        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,e.getMessage());
        }
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

              if (detail.getCredit().compareTo(BigDecimal.ZERO) != 0){
                    entity.setAmount(detail.getCredit());
                    entity.setMotion(Motion.C);
                    entity.setTransaction(transactionEntity);
                  result.add(entity);
                }else  if (detail.getDebit().compareTo(BigDecimal.ZERO) != 0){
                  entity.setAmount(detail.getDebit());
                  entity.setMotion(Motion.D);
                  entity.setTransaction(transactionEntity);
                  result.add(entity);
                }
//              else {
//                  entity.setAmount(BigDecimal.ZERO);
//                  entity.setMotion(null);
//                  entity.setTransaction(transactionEntity);
//              }

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
        bulkAccountConfigRequest.setTenantId("123456");
        bulkAccountConfigRequest.setType(request.getType());

        return bulkAccountConfigRequest;
    }


    private static List<BulkTransactionResponse> getAllBulkAccountsConfigResponse(List<BulkAccountConfig> bulkAccountConfig) {

        List<BulkTransactionResponse> result = new ArrayList<>();

        for (BulkAccountConfig bulkAccount: bulkAccountConfig){

            BulkTransactionResponse uploadBulkTransactionResponse = new BulkTransactionResponse();
            uploadBulkTransactionResponse.setId(bulkAccount.getId());
            uploadBulkTransactionResponse.setStatus(bulkAccount.getIsActive());
            uploadBulkTransactionResponse.setName(bulkAccount.getName());
            uploadBulkTransactionResponse.setRowInit(bulkAccount.getRowStart());
            uploadBulkTransactionResponse.setTenantId(bulkAccount.getTenantId());
            uploadBulkTransactionResponse.setType(bulkAccount.getType());

            List<BulkTransactionRequestList> uploadBulkTransactionResponseList =  new ArrayList<>();

            for (BulkAccountConfigDetail bulkAccountConfigDetail:bulkAccount.getDetails()){
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

            List<BulkTransactionRequestList> uploadBulkTransactionResponseList =  new ArrayList<>();

            for (BulkAccountConfigDetail bulkAccountConfigDetail:bulkAccount.getDetails()){
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

    public static boolean esNumeroValido(String cadena) {
        if (cadena == null || cadena.isEmpty()) {
            return false;
        }
        String patron = "^[+-]?\\d+(\\.\\d+)?$";
        return cadena.matches(patron);
    }

    public static boolean esFechaValida(String fechaStr) {
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

    public static String errorMessage(String description,String message ) {
         return description == null ||
                description.isEmpty()
                ? message
                : description+","+ message;

    }




}
