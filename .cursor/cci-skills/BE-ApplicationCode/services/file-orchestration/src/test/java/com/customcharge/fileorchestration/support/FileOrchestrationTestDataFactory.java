package com.customcharge.fileorchestration.support;

import com.customcharge.fileorchestration.config.BatchFlowProperties;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FileOrchestrationTestDataFactory {

    private FileOrchestrationTestDataFactory() {
    }

    public static BatchFlowProperties batchFlowProperties() {
        BatchFlowProperties properties = new BatchFlowProperties();
        properties.setInputQueue("input-queue");
        properties.setOutputQueue("output-queue");
        properties.setChunkSize(200);
        properties.setSupportedFileTypes(List.of("CSV", "XLSX"));
        properties.setDedupEnabled(true);
        return properties;
    }

    public static String validS3UploadEvent() {
        return """
                {
                  "Records" : [ {
                    "eventVersion" : "2.1",
                    "eventSource" : "aws:s3",
                    "awsRegion" : "ap-south-1",
                    "eventTime" : "2026-06-15T04:14:34.943Z",
                    "eventName" : "ObjectCreated:Put",
                    "s3" : {
                      "bucket" : {
                        "name" : "mvp-demo-bucket-v2"
                      },
                      "object" : {
                        "key" : "uploaded-files/c5f29284-237c-4079-bb37-0107d8aeac38/Estimates-Upload-Template-RUH+%281%29.xlsx"
                      }
                    }
                  } ]
                }
                """;
    }

    public static Map<String, String> row(String awb, String saddad) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("awb", awb);
        row.put("saddad", saddad);
        return row;
    }

    public static InputStream csvStream(String csvContent) {
        return new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
    }

    public static InputStream xlsxStream(String[][] rows) throws IOException {
        return new ByteArrayInputStream(xlsxBytes(rows));
    }

    public static byte[] xlsxBytes(String[][] rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                Row row = sheet.createRow(rowIndex);
                for (int colIndex = 0; colIndex < rows[rowIndex].length; colIndex++) {
                    row.createCell(colIndex).setCellValue(rows[rowIndex][colIndex]);
                }
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
