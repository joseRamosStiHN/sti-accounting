package com.sti.accounting.services;


import com.sti.accounting.entities.BulkAccountConfig;
import com.sti.accounting.entities.BulkAccountConfigDetail;
import com.sti.accounting.repositories.IBulkAccountConfigRepository;
import lombok.val;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Service
public class BulkAccountConfigService {

    private static final Logger logger = LoggerFactory.getLogger(BulkAccountConfigService.class);

    private final IBulkAccountConfigRepository bulkAccountConfigRepository;

    public BulkAccountConfigService(IBulkAccountConfigRepository bulkAccountConfigRepository) {
        this.bulkAccountConfigRepository = bulkAccountConfigRepository;
    }

    public void  ExcelToObject(MultipartFile file) {

        // get configuration        /// Id y tenant ID
        BulkAccountConfig configs = this.bulkAccountConfigRepository.findById(1L)
                                            .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));


        //start row
        int startRow = configs.getRowStart() - 1;
        try(InputStream inputStream = file.getInputStream()){
            Workbook workbook = new XSSFWorkbook(inputStream);

            Sheet sheet = workbook.getSheetAt(1); // get first book

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (int i = startRow; i < sheet.getLastRowNum(); i++) {
                //get cell value
                for (BulkAccountConfigDetail detail : configs.getDetails()) {
                    CellType cellType = sheet.getRow(i).getCell(detail.getColIndex()).getCellType();
                    DataFormatter formatter = new DataFormatter();
                    String strValue =  "";
                    if(cellType == CellType.FORMULA) {
                        strValue = String.valueOf(evaluator.evaluate(sheet.getRow(i).getCell(detail.getColIndex())).getNumberValue());
                    }else{
                        strValue = formatter.formatCellValue(sheet.getRow(i).getCell(detail.getColIndex()));
                    }
                    logger.info("Cell type {} cell value {} row: {} col: {}", cellType, strValue, sheet.getRow(i).getRowNum() + 1 , detail.getColIndex());

                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
