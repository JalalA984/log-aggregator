package app.vercel.jalalahmad.log_query_service.controller;

import app.vercel.jalalahmad.log_query_service.model.IndexRequestDTO;
import app.vercel.jalalahmad.log_query_service.model.IndexedLog;
import app.vercel.jalalahmad.log_query_service.model.SearchRequestDTO;
import app.vercel.jalalahmad.log_query_service.service.LogIndexingService;
import app.vercel.jalalahmad.log_query_service.service.LogSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import app.vercel.jalalahmad.log_query_service.repository.IndexedLogRepository;

@RestController
@RequestMapping("/query")
public class LogQueryController {

    @Autowired
    private LogIndexingService logIndexingService;

    @Autowired
    private LogSearchService logSearchService;

    @Autowired
    private IndexedLogRepository indexedLogRepository;

    // This endpoint is called by Ingest Service asynchronously
    @PostMapping("/index")
    public ResponseEntity<Map<String, Object>> indexLog(@RequestBody IndexRequestDTO indexRequest) {
        try {
            IndexedLog indexedLog = logIndexingService.indexLog(indexRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Log indexed successfully");
            response.put("logId", indexedLog.getLogId());
            response.put("indexedAt", indexedLog.getIndexedAt());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to index log: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Search logs with filters
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchLogs(@RequestBody SearchRequestDTO searchRequest) {
        try {
            Page<IndexedLog> logsPage = logSearchService.searchLogs(searchRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("logs", logsPage.getContent());
            response.put("totalPages", logsPage.getTotalPages());
            response.put("totalElements", logsPage.getTotalElements());
            response.put("currentPage", logsPage.getNumber());
            response.put("pageSize", logsPage.getSize());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Search failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Get a specific log by ID
    @GetMapping("/log/{id}")
    public ResponseEntity<Map<String, Object>> getLogById(@PathVariable String id) {
        try {
            IndexedLog log = logIndexingService.getLogById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("log", log);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    // Get statistics
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam(required = false) String hours) {
        try {
            int hoursBack = hours != null ? Integer.parseInt(hours) : 24;
            LocalDateTime from = LocalDateTime.now().minusHours(hoursBack);
            LocalDateTime to = LocalDateTime.now();

            Map<String, Object> stats = logSearchService.getStatistics(from, to);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", stats);
            response.put("timeRange", Map.of("from", from, "to", to));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Health check
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "log-query-service");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("indexedLogsCount", indexedLogRepository.count());
        return ResponseEntity.ok(response);
    }
}