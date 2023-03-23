package org.example.CarbonStorehouseBot.service;

import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.*;
import org.example.CarbonStorehouseBot.model.Roll;
import org.example.CarbonStorehouseBot.repository.FabricRepository;
import org.example.CarbonStorehouseBot.repository.RollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

@Component
public class WriteFile {

    @Autowired
    private RollRepository rollRepository;
    @Autowired
    private FabricRepository fabricRepository;
    private final String[] headersFabric = {"Номер партии", "Название партии", "Метраж партии", "Дата изготовления", "Фактический метраж партии", "Статус партии"};
    private final String[] headersRoll = {"Номер руллона", "Метраж руллона", "Заметка", "Дата изготовления", "Статус руллона", "Номер партии"};

    protected void writeExcelFileReadyFabric(Map<List<Object[]>, List<Roll>> allDataFabric, String messageText){
        if(messageText.equals("Проданные партии")){
            headersFabric[3] = "Дата продажи";
            headersRoll[3] = "Дата продажи";
        }
        // Создаем новый Excel-файл
        XSSFWorkbook workbook = new XSSFWorkbook();
        // Задаем стиль, текст в ячейке по центру
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        // Создаем шапку
        XSSFSheet sheet = workbook.createSheet("Партия");
        XSSFRow headerFabricRow = sheet.createRow(0);
        for (int i = 0; i < headersFabric.length; i++) {
            XSSFCell cell = headerFabricRow.createCell(i);
            cell.setCellValue(headersFabric[i]);
            cell.setCellStyle(cellStyle);
            sheet.autoSizeColumn(i);
        }

        XSSFSheet sheet2 = workbook.createSheet("Руллоны");
        XSSFRow headerRollRow = sheet2.createRow(0);
        for (int i = 0; i < headersRoll.length; i++) {
            XSSFCell cell = headerRollRow.createCell(i);
            cell.setCellValue(headersRoll[i]);
            cell.setCellStyle(cellStyle);
            sheet.autoSizeColumn(i);
        }

        // Заполняем данными
        int rowIndexFabric = 1;
        int rowIndexRoll = 1;

        for (Map.Entry<List<Object[]>, List<Roll>> entry : allDataFabric.entrySet()){
            List<Object[]> fabricAndMetricData = entry.getKey();
            List<Roll> rollList = entry.getValue();

            for (Object[] dataFabric : fabricAndMetricData) {
                XSSFRow dataRow = sheet.createRow(rowIndexRoll++);
                int cellIndex = 0;

                XSSFCell cell1 = dataRow.createCell(cellIndex++);
                cell1.setCellValue(dataFabric[0].toString());
                cell1.setCellStyle(cellStyle);
                sheet.autoSizeColumn(cellIndex);

                XSSFCell cell2 = dataRow.createCell(cellIndex++);
                cell2.setCellValue(dataFabric[1].toString());
                cell2.setCellStyle(cellStyle);
                sheet.autoSizeColumn(cellIndex);

                XSSFCell cell3 = dataRow.createCell(cellIndex++);
                cell3.setCellValue(dataFabric[3].toString());
                cell3.setCellStyle(cellStyle);
                sheet.autoSizeColumn(cellIndex);

                XSSFCell cell4 = dataRow.createCell(cellIndex++);
                cell4.setCellValue(dataFabric[4].toString());
                cell4.setCellStyle(cellStyle);
                sheet.autoSizeColumn(cellIndex);

                XSSFCell cell5 = dataRow.createCell(cellIndex++);
                cell5.setCellValue(dataFabric[5].toString());
                cell5.setCellStyle(cellStyle);
                sheet.autoSizeColumn(cellIndex);

                XSSFCell cell6 = dataRow.createCell(cellIndex++);
                cell6.setCellValue(dataFabric[2].toString());
                cell6.setCellStyle(cellStyle);
                sheet.autoSizeColumn(cellIndex);
            }

            for (Roll roll : rollList) {
                XSSFRow dataRow = sheet2.createRow(rowIndexFabric++);
                int cellIndex = 0;

                XSSFCell cell1 = dataRow.createCell(cellIndex++);
                cell1.setCellValue(roll.getNumberRoll());
                cell1.setCellStyle(cellStyle);
                sheet2.autoSizeColumn(cellIndex);

                XSSFCell cell2 = dataRow.createCell(cellIndex++);
                cell2.setCellValue(roll.getRollMetric());
                cell2.setCellStyle(cellStyle);
                sheet2.autoSizeColumn(cellIndex);

                XSSFCell cell3 = dataRow.createCell(cellIndex++);
                cell3.setCellValue(roll.getRemark());
                cell3.setCellStyle(cellStyle);
                sheet2.autoSizeColumn(cellIndex);

                XSSFCell cell4 = dataRow.createCell(cellIndex++);
                cell4.setCellValue(roll.getDateFulfilment().toString());
                cell4.setCellStyle(cellStyle);
                sheet2.autoSizeColumn(cellIndex);

                XSSFCell cell5 = dataRow.createCell(cellIndex++);
                cell5.setCellValue(roll.getStatusRoll().toString());
                cell5.setCellStyle(cellStyle);
                sheet2.autoSizeColumn(cellIndex);

                XSSFCell cell6 = dataRow.createCell(cellIndex++);
                cell6.setCellValue(roll.getFabric().getBatchNumberId());
                cell6.setCellStyle(cellStyle);
                sheet2.autoSizeColumn(cellIndex);
            }
            sheet2.createRow(rowIndexFabric++); //пробел
        }
        // Сохраняем файл
        try {
            FileOutputStream outputStream = new FileOutputStream("C:/doc/" + messageText + ".xlsx");
            workbook.write(outputStream);
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
