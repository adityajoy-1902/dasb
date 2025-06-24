package com.example.dashboard.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnsibleRequest {
    private String serverName;
    private String ipAddress;
    private String username;
    private String password;
    private String sshKeyPath;
    private List<String> servicesToCheck; // TOMCAT, IIS, JBOSS
    private boolean useSudo;
    private int timeout;
    private String ansibleInventoryPath;
} 