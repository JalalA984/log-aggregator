package app.vercel.jalalahmad.log_ingest_service.controller;

import app.vercel.jalalahmad.log_ingest_service.model.LogEntry;
import app.vercel.jalalahmad.log_ingest_service.model.LogRequestDTO;
import app.vercel.jalalahmad.log_ingest_service.service.LogIngestionService;
import app.vercel.jalalahmad.log_ingest_service.service.RawLogIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ingest")
public class LogIngestController {

    @Autowired
    private LogIngestionService logIngestionService;

    @Autowired
    private RawLogIngestionService rawLogIngestionService;

    @PostMapping("/log")
    public ResponseEntity<Map<String, Object>> ingestSingleLog(@RequestBody LogRequestDTO logRequest) {
        try {
            LogEntry savedEntry = logIngestionService.ingestLog(logRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Log ingested successfully");
            response.put("logId", savedEntry.getId());

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to ingest log: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/logs/batch")
    public ResponseEntity<Map<String, Object>> ingestBatchLogs(@RequestBody List<LogRequestDTO> logRequests) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Batch ingestion started");

            // Process each log asynchronously
            for (LogRequestDTO logRequest : logRequests) {
                logIngestionService.ingestLog(logRequest);
            }

            response.put("processedCount", logRequests.size());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to ingest batch logs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/log/{id}")
    public ResponseEntity<Map<String, Object>> getLogById(@PathVariable Long id) {
        try {
            LogEntry logEntry = logIngestionService.getLogById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("log", logEntry);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @PostMapping("/raw/{schemaName}")
    public ResponseEntity<Map<String, Object>> ingestRawLog(
            @PathVariable String schemaName,
            @RequestBody Map<String, Object> rawLog) {
        try {
            LogEntry saved = rawLogIngestionService.ingestRawLog(schemaName, rawLog);

            Map<String, Object> response = new HashMap<>();
            if (saved != null) {
                response.put("success", true);
                response.put("message", "Log ingested via schema: " + schemaName);
                response.put("logId", saved.getId());
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            } else {
                response.put("success", false);
                response.put("message", "Log ignored (validation failed or schema not found)");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to ingest raw log: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/raw/{schemaName}/batch")
    public ResponseEntity<Map<String, Object>> ingestRawLogBatch(
            @PathVariable String schemaName,
            @RequestBody List<Map<String, Object>> rawLogs) {
        int accepted = 0;
        int ignored = 0;
        for (Map<String, Object> rawLog : rawLogs) {
            try {
                LogEntry saved = rawLogIngestionService.ingestRawLog(schemaName, rawLog);
                if (saved != null) accepted++; else ignored++;
            } catch (Exception e) {
                ignored++;
            }
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("accepted", accepted);
        response.put("ignored", ignored);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "log-ingest-service");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}