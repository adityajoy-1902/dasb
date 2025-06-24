package com.example.dashboard.controller;

import com.example.dashboard.model.ServerStatus;
import com.example.dashboard.model.AnsibleRequest;
import com.example.dashboard.service.AnsibleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ansible")
@RequiredArgsConstructor
@Slf4j
public class AnsibleController {

    private final AnsibleService ansibleService;

    @PostMapping("/check-status")
    public ResponseEntity<ServerStatus> checkServerStatus(@RequestBody AnsibleRequest request) {
        log.info("Checking status for server: {} ({})", request.getServerName(), request.getIpAddress());
        
        try {
            ServerStatus status = ansibleService.checkServerStatus(request);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error checking server status: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/check-multiple")
    public ResponseEntity<List<ServerStatus>> checkMultipleServers(@RequestBody List<AnsibleRequest> requests) {
        log.info("Checking status for {} servers", requests.size());
        
        try {
            List<ServerStatus> statuses = requests.stream()
                .map(ansibleService::checkServerStatus)
                .toList();
            return ResponseEntity.ok(statuses);
        } catch (Exception e) {
            log.error("Error checking multiple server statuses: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "Ansible Status Checker",
            "version", "1.0.0"
        ));
    }

    @GetMapping("/supported-services")
    public ResponseEntity<Map<String, Object>> getSupportedServices() {
        Map<String, Object> services = Map.of(
            "supported_services", List.of("TOMCAT", "IIS", "JBOSS"),
            "description", "Server status checking service using Ansible",
            "features", List.of(
                "Process status checking",
                "Version detection",
                "Port monitoring",
                "Memory usage tracking",
                "Service uptime monitoring"
            )
        );
        return ResponseEntity.ok(services);
    }
} 