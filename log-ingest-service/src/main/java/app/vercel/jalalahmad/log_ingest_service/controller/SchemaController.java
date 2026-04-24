package app.vercel.jalalahmad.log_ingest_service.controller;

import app.vercel.jalalahmad.log_ingest_service.model.LogSchema;
import app.vercel.jalalahmad.log_ingest_service.model.SchemaInferenceResult;
import app.vercel.jalalahmad.log_ingest_service.service.SchemaManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ingest/schemas")
public class SchemaController {

    @Autowired
    private SchemaManagementService schemaManagementService;

    @PostMapping("/infer")
    public ResponseEntity<Map<String, Object>> inferSchema(@RequestBody Map<String, String> request) {
        try {
            String sampleLog = request.get("sampleLog");
            if (sampleLog == null || sampleLog.isBlank()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "sampleLog is required");
                return ResponseEntity.badRequest().body(error);
            }

            SchemaInferenceResult result = schemaManagementService.inferSchema(sampleLog);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", result);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to parse sample log: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSchema(@RequestBody LogSchema schema) {
        try {
            LogSchema saved = schemaManagementService.createSchema(schema);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Schema created successfully");
            response.put("schema", saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to create schema: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllSchemas() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("schemas", schemaManagementService.getAllSchemas());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{name}")
    public ResponseEntity<Map<String, Object>> getSchemaByName(@PathVariable String name) {
        try {
            LogSchema schema = schemaManagementService.getSchemaByName(name);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("schema", schema);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateSchema(@PathVariable UUID id, @RequestBody LogSchema schema) {
        try {
            LogSchema updated = schemaManagementService.updateSchema(id, schema);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Schema updated successfully");
            response.put("schema", updated);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteSchema(@PathVariable UUID id) {
        try {
            schemaManagementService.deleteSchema(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Schema deleted successfully");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}
