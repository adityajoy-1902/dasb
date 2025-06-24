package com.example.dashboard.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerStatus {
    private String serverName;
    private String ipAddress;
    private String os;
    private String status; // ONLINE, OFFLINE, ERROR
    private String lastChecked;
    private List<ServiceStatus> services;
    private Map<String, Object> additionalInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceStatus {
        private String serviceName;
        private String serviceType; // TOMCAT, IIS, JBOSS, etc.
        private String status; // RUNNING, STOPPED, ERROR
        private String port;
        private String version;
        private String uptime;
        private String memoryUsage;
        private String cpuUsage;
        private String lastRestart;
        private Map<String, Object> metrics;
    }
} 