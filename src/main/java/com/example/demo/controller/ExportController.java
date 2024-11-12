package com.example.demo.controller;

import com.example.demo.service.BotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private BotService botService;

    @GetMapping("/csv")
    public ResponseEntity<String> exportToCsv() {
        try {
            String filePath = "patients_data.csv";
            botService.exportToCSV(filePath);
            return ResponseEntity.ok("CSV export completed successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error exporting to CSV");
        }
    }

    @GetMapping("/excel")
    public ResponseEntity<String> exportToExcel() {
        try {
            String filePath = "patients_data.xlsx";
            botService.exportToExcel(filePath);
            return ResponseEntity.ok("Excel export completed successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error exporting to Excel");
        }
    }
}