package org.example.CarbonStorehouseBot.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.*;
import org.example.CarbonStorehouseBot.model.Roll;
import org.example.CarbonStorehouseBot.model.RollsColleaguesTable;
import org.example.CarbonStorehouseBot.repository.FabricRepository;
import org.example.CarbonStorehouseBot.repository.RollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WriteFile {

    @Autowired
    private RollRepository rollRepository;
    @Autowired
    private FabricRepository fabricRepository;
    private static final String[] headersFabric = {"Номер партии", "Название партии", "Метраж партии", "Дата изготовления/продажи", "Фактический метраж партии", "Статус партии"};
    private static final String[] headersRoll = {"Рулон", "Метраж рулона", "Заметка", "Дата изготовления/продажи", "Статус рулона", "Номер партии"};
    private static final String[] headersNameAndMetricColleague = {"Имя сотрудника", "Метраж за месяц"};
    private static final String[] headersAllManufactureColleague = {"Партия", "Рулон", "Метраж рулона", "Дата рабочей смены"};

    protected void writeExcelFileReadyFabric(Map<List<Object[]>, List<Roll>> allDataFabric, String messageText){
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

        XSSFSheet sheet2 = workbook.createSheet("Рулоны");
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
            log.error("ERROR_TEXT " + e.getMessage());
        }
    }

    protected void writeExcelFileManufactureColleague(Map<List<Object[]>, List<RollsColleaguesTable>> allDataColleague, String messageText){
        XSSFWorkbook workbook = new XSSFWorkbook();
        // Задаем стиль, текст в ячейке по центру
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        // Создаем шапку
        XSSFSheet sheet = workbook.createSheet("Метраж за месяц");
        XSSFRow headerFabricRow = sheet.createRow(0);
        for (int i = 0; i < headersNameAndMetricColleague.length; i++) {
            XSSFCell cell = headerFabricRow.createCell(i);
            cell.setCellValue(headersNameAndMetricColleague[i]);
            cell.setCellStyle(cellStyle);
            sheet.autoSizeColumn(i);
        }

        XSSFSheet sheet2 = workbook.createSheet("Подробные данные");
        XSSFRow headerRollRow = sheet2.createRow(0);
        for (int i = 0; i < headersAllManufactureColleague.length; i++) {
            XSSFCell cell = headerRollRow.createCell(i);
            cell.setCellValue(headersAllManufactureColleague[i]);
            cell.setCellStyle(cellStyle);
            sheet.autoSizeColumn(i);
        }

        // Заполняем данными
        int rowIndexNameAndMetric = 1;
        int rowIndexAllDataManufacture = 1;

        for (Map.Entry<List<Object[]>, List<RollsColleaguesTable>> entry : allDataColleague.entrySet()){
            List<Object[]> nameAndMetricColleague = entry.getKey();
            List<RollsColleaguesTable> allDataManufactureColleague = entry.getValue();

            for (Object[] nameAndMetric : nameAndMetricColleague) {
                XSSFRow dataRow = sheet.createRow(rowIndexNameAndMetric++);
                int cellIndex = 0;

                XSSFCell cell1 = dataRow.createCell(cellIndex++);
                cell1.setCellValue(nameAndMetric[0].toString());
                cell1.setCellStyle(cellStyle);
                sheet.autoSizeColumn(cellIndex);

                XSSFCell cell2 = dataRow.createCell(cellIndex++);
                cell2.setCellValue(nameAndMetric[1].toString());
                cell2.setCellStyle(cellStyle);
                sheet.autoSizeColumn(cellIndex);
            }

            for (RollsColleaguesTable rollsColleaguesTable : allDataManufactureColleague) {
                XSSFRow dataRow = sheet2.createRow(rowIndexAllDataManufacture++);
                int cellIndex2 = 0;

                XSSFCell cell = dataRow.createCell(cellIndex2++);
                cell.setCellValue(rollsColleaguesTable.getNumberFabric());
                cell.setCellStyle(cellStyle);
                sheet2.autoSizeColumn(cellIndex2);

                XSSFCell cell1 = dataRow.createCell(cellIndex2++);
                cell1.setCellValue(rollsColleaguesTable.getNumberRoll());
                cell1.setCellStyle(cellStyle);
                sheet2.autoSizeColumn(cellIndex2);

                XSSFCell cell2 = dataRow.createCell(cellIndex2++);
                cell2.setCellValue(rollsColleaguesTable.getMetricColleague());
                cell2.setCellStyle(cellStyle);
                sheet2.autoSizeColumn(cellIndex2);

                XSSFCell cell3 = dataRow.createCell(cellIndex2++);
                cell3.setCellValue(rollsColleaguesTable.getDateWorkingShift().toString());
                cell3.setCellStyle(cellStyle);
                sheet2.autoSizeColumn(cellIndex2);
            }
        }
        // Сохраняем файл
        try {
            FileOutputStream outputStream = new FileOutputStream("C:/doc/" + messageText + ".xlsx");
            workbook.write(outputStream);
            workbook.close();
        } catch (Exception e) {
            log.error("ERROR_TEXT " + e.getMessage());
        }
    }

}
