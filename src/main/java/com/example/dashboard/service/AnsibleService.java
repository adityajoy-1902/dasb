package com.example.dashboard.service;

import com.example.dashboard.model.ServerStatus;
import com.example.dashboard.model.AnsibleRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AnsibleService {

    private static final String ANSIBLE_PLAYBOOK_DIR = "ansible-playbooks";
    private static final String INVENTORY_FILE = "inventory.ini";
    private static final String ANSIBLE_CONFIG = "ansible.cfg";

    public ServerStatus checkServerStatus(AnsibleRequest request) {
        try {
            // Create temporary inventory file
            String inventoryPath = createInventoryFile(request);
            
            // Create playbook for the requested services
            String playbookPath = createStatusCheckPlaybook(request);
            
            // Execute Ansible playbook
            String result = executeAnsiblePlaybook(inventoryPath, playbookPath, request);
            
            // Parse the result and create ServerStatus
            return parseAnsibleResult(result, request);
            
        } catch (Exception e) {
            log.error("Error checking server status for {}: {}", request.getServerName(), e.getMessage());
            return createErrorStatus(request, e.getMessage());
        }
    }

    private String createInventoryFile(AnsibleRequest request) throws IOException {
        Path inventoryDir = Paths.get(ANSIBLE_PLAYBOOK_DIR);
        if (!Files.exists(inventoryDir)) {
            Files.createDirectories(inventoryDir);
        }

        String inventoryContent = String.format(
            "[servers]\n" +
            "%s ansible_host=%s ansible_user=%s",
            request.getServerName(),
            request.getIpAddress(),
            request.getUsername()
        );

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            inventoryContent += String.format(" ansible_password=%s", request.getPassword());
        }

        if (request.getSshKeyPath() != null && !request.getSshKeyPath().isEmpty()) {
            inventoryContent += String.format(" ansible_ssh_private_key_file=%s", request.getSshKeyPath());
        }

        if (request.isUseSudo()) {
            inventoryContent += " ansible_become=yes ansible_become_method=sudo";
        }

        inventoryContent += "\n\n[all:vars]\nansible_connection=ssh\nansible_ssh_common_args='-o StrictHostKeyChecking=no'";

        Path inventoryPath = inventoryDir.resolve(INVENTORY_FILE);
        Files.write(inventoryPath, inventoryContent.getBytes());
        
        return inventoryPath.toString();
    }

    private String createStatusCheckPlaybook(AnsibleRequest request) throws IOException {
        Path playbookDir = Paths.get(ANSIBLE_PLAYBOOK_DIR);
        if (!Files.exists(playbookDir)) {
            Files.createDirectories(playbookDir);
        }

        StringBuilder playbookContent = new StringBuilder();
        playbookContent.append("---\n");
        playbookContent.append("- name: Check Server Status\n");
        playbookContent.append("  hosts: servers\n");
        playbookContent.append("  gather_facts: yes\n");
        playbookContent.append("  tasks:\n");

        // Add tasks for each service type
        for (String service : request.getServicesToCheck()) {
            switch (service.toUpperCase()) {
                case "TOMCAT":
                    addTomcatTasks(playbookContent);
                    break;
                case "IIS":
                    addIISTasks(playbookContent);
                    break;
                case "JBOSS":
                    addJBossTasks(playbookContent);
                    break;
            }
        }

        Path playbookPath = playbookDir.resolve("status_check.yml");
        Files.write(playbookPath, playbookContent.toString().getBytes());
        
        return playbookPath.toString();
    }

    private void addTomcatTasks(StringBuilder playbookContent) {
        playbookContent.append("    - name: Check if Tomcat is running\n");
        playbookContent.append("      shell: ps aux | grep -E '(tomcat|catalina)' | grep -v grep\n");
        playbookContent.append("      register: tomcat_process\n");
        playbookContent.append("      ignore_errors: yes\n");
        playbookContent.append("      changed_when: false\n\n");

        playbookContent.append("    - name: Get Tomcat version (multiple paths)\n");
        playbookContent.append("      shell: |\n");
        playbookContent.append("        if [ -f /opt/tomcat/bin/version.sh ]; then\n");
        playbookContent.append("          /opt/tomcat/bin/version.sh\n");
        playbookContent.append("        elif [ -f /usr/share/tomcat*/bin/version.sh ]; then\n");
        playbookContent.append("          /usr/share/tomcat*/bin/version.sh\n");
        playbookContent.append("        elif [ -f /data/tomcat/bin/version.sh ]; then\n");
        playbookContent.append("          /data/tomcat/bin/version.sh\n");
        playbookContent.append("        elif [ -f /usr/local/tomcat/bin/version.sh ]; then\n");
        playbookContent.append("          /usr/local/tomcat/bin/version.sh\n");
        playbookContent.append("        elif [ -f /opt/apache-tomcat*/bin/version.sh ]; then\n");
        playbookContent.append("          /opt/apache-tomcat*/bin/version.sh\n");
        playbookContent.append("        else\n");
        playbookContent.append("          find /opt -name 'catalina.sh' -exec {} version \\; 2>/dev/null | head -1\n");
        playbookContent.append("        fi\n");
        playbookContent.append("      register: tomcat_version\n");
        playbookContent.append("      ignore_errors: yes\n");
        playbookContent.append("      changed_when: false\n\n");

        playbookContent.append("    - name: Check Tomcat ports (multiple ranges)\n");
        playbookContent.append("      shell: netstat -tlnp | grep java | grep -E ':(8080|8443|8005|8009|8006)'\n");
        playbookContent.append("      register: tomcat_ports\n");
        playbookContent.append("      ignore_errors: yes\n");
        playbookContent.append("      changed_when: false\n\n");

        playbookContent.append("    - name: Get Tomcat memory usage\n");
        playbookContent.append("      shell: ps aux | grep -E '(tomcat|catalina)' | grep -v grep | awk '{print $6}' | head -1\n");
        playbookContent.append("      register: tomcat_memory\n");
        playbookContent.append("      ignore_errors: yes\n");
        playbookContent.append("      changed_when: false\n\n");

        playbookContent.append("    - name: Check Tomcat installation directories\n");
        playbookContent.append("      shell: |\n");
        playbookContent.append("        echo 'Checking common Tomcat paths:'\n");
        playbookContent.append("        ls -la /opt/ | grep -i tomcat || echo 'No tomcat in /opt/'\n");
        playbookContent.append("        ls -la /usr/share/ | grep -i tomcat || echo 'No tomcat in /usr/share/'\n");
        playbookContent.append("        ls -la /data/ | grep -i tomcat || echo 'No tomcat in /data/'\n");
        playbookContent.append("        ls -la /usr/local/ | grep -i tomcat || echo 'No tomcat in /usr/local/'\n");
        playbookContent.append("      register: tomcat_paths\n");
        playbookContent.append("      ignore_errors: yes\n");
        playbookContent.append("      changed_when: false\n\n");
    }

    private void addIISTasks(StringBuilder playbookContent) {
        playbookContent.append("    - name: Check IIS service status (Windows)\n");
        playbookContent.append("      win_service:\n");
        playbookContent.append("        name: W3SVC\n");
        playbookContent.append("      register: iis_service\n");
        playbookContent.append("      ignore_errors: yes\n\n");

        playbookContent.append("    - name: Get IIS version (Windows)\n");
        playbookContent.append("      win_shell: Get-ItemProperty -Path 'HKLM:\\SOFTWARE\\Microsoft\\InetStp' -Name VersionString\n");
        playbookContent.append("      register: iis_version\n");
        playbookContent.append("      ignore_errors: yes\n\n");

        playbookContent.append("    - name: Check IIS application pools (Windows)\n");
        playbookContent.append("      win_shell: Get-IISAppPool | Select-Object Name, State\n");
        playbookContent.append("      register: iis_app_pools\n");
        playbookContent.append("      ignore_errors: yes\n\n");
    }

    private void addJBossTasks(StringBuilder playbookContent) {
        playbookContent.append("    - name: Check if JBoss is running\n");
        playbookContent.append("      shell: ps aux | grep jboss | grep -v grep\n");
        playbookContent.append("      register: jboss_process\n");
        playbookContent.append("      ignore_errors: yes\n");
        playbookContent.append("      changed_when: false\n\n");

        playbookContent.append("    - name: Get JBoss version\n");
        playbookContent.append("      shell: find /opt -name 'standalone.sh' -exec dirname {} \\; | xargs -I {} find {} -name 'jboss-modules.jar' -exec java -jar {} --version \\; 2>/dev/null\n");
        playbookContent.append("      register: jboss_version\n");
        playbookContent.append("      ignore_errors: yes\n");
        playbookContent.append("      changed_when: false\n\n");

        playbookContent.append("    - name: Check JBoss ports\n");
        playbookContent.append("      shell: netstat -tlnp | grep java | grep -E ':(8080|8443|9990|9999)'\n");
        playbookContent.append("      register: jboss_ports\n");
        playbookContent.append("      ignore_errors: yes\n");
        playbookContent.append("      changed_when: false\n\n");

        playbookContent.append("    - name: Get JBoss memory usage\n");
        playbookContent.append("      shell: ps aux | grep jboss | grep -v grep | awk '{print $6}' | head -1\n");
        playbookContent.append("      register: jboss_memory\n");
        playbookContent.append("      ignore_errors: yes\n");
        playbookContent.append("      changed_when: false\n\n");
    }

    private String executeAnsiblePlaybook(String inventoryPath, String playbookPath, AnsibleRequest request) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
            "ansible-playbook",
            "-i", inventoryPath,
            playbookPath,
            "--verbose"
        );

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // Read output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        // Wait for completion with timeout
        boolean completed = process.waitFor(request.getTimeout(), TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("Ansible playbook execution timed out");
        }

        int exitCode = process.exitValue();
        String outputStr = output.toString();
        
        // Log the full output for debugging
        log.debug("Ansible playbook output: {}", outputStr);
        
        if (exitCode != 0) {
            log.warn("Ansible playbook exited with code: {}", exitCode);
            
            // Provide specific error messages based on common issues
            if (outputStr.contains("Permission denied (publickey)")) {
                throw new RuntimeException("SSH authentication failed: Server requires SSH key authentication. Please provide sshKeyPath instead of password.");
            } else if (outputStr.contains("Connection refused")) {
                throw new RuntimeException("SSH connection refused: Server may be down or SSH service not running.");
            } else if (outputStr.contains("No route to host")) {
                throw new RuntimeException("Network connectivity issue: Cannot reach the target server.");
            } else if (outputStr.contains("sshpass")) {
                throw new RuntimeException("SSH password authentication requires sshpass to be installed.");
            }
        }

        return outputStr;
    }

    private ServerStatus parseAnsibleResult(String result, AnsibleRequest request) {
        ServerStatus.ServerStatusBuilder builder = ServerStatus.builder()
            .serverName(request.getServerName())
            .ipAddress(request.getIpAddress())
            .lastChecked(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        List<ServerStatus.ServiceStatus> services = new ArrayList<>();

        // Parse service statuses based on the result
        for (String serviceType : request.getServicesToCheck()) {
            ServerStatus.ServiceStatus serviceStatus = parseServiceStatus(result, serviceType);
            if (serviceStatus != null) {
                services.add(serviceStatus);
            }
        }

        builder.services(services);
        
        // Determine overall server status
        String overallStatus = determineOverallStatus(services);
        builder.status(overallStatus);

        return builder.build();
    }

    private ServerStatus.ServiceStatus parseServiceStatus(String result, String serviceType) {
        ServerStatus.ServiceStatus.ServiceStatusBuilder builder = ServerStatus.ServiceStatus.builder()
            .serviceType(serviceType.toUpperCase());

        switch (serviceType.toUpperCase()) {
            case "TOMCAT":
                return parseTomcatStatus(result, builder);
            case "IIS":
                return parseIISStatus(result, builder);
            case "JBOSS":
                return parseJBossStatus(result, builder);
            default:
                return null;
        }
    }

    private ServerStatus.ServiceStatus parseTomcatStatus(String result, ServerStatus.ServiceStatus.ServiceStatusBuilder builder) {
        builder.serviceName("Tomcat");

        // Check if Tomcat process is running (improved detection for Tomcat 10)
        boolean tomcatRunning = false;
        
        // Check for tomcat user processes (specific to this server)
        if (result.contains("tomcat_process") && !result.contains("tomcat_process.stdout_lines: []")) {
            // Look for tomcat user processes
            if (result.contains("tomcat") && result.contains("java")) {
                tomcatRunning = true;
            }
        }
        
        // Also check for java processes that might be Tomcat
        if (!tomcatRunning && result.contains("java") && result.contains("catalina")) {
            tomcatRunning = true;
        }
        
        // Check for specific Tomcat 10 installation paths
        if (!tomcatRunning && (result.contains("/usr/share/tomcat10") || result.contains("/var/lib/tomcat10"))) {
            tomcatRunning = true;
        }

        if (tomcatRunning) {
            builder.status("RUNNING");
            
            // Extract version (improved for Tomcat 10.1.34)
            Pattern versionPattern = Pattern.compile("Server version:\\s*([^\\n]+)");
            Matcher versionMatcher = versionPattern.matcher(result);
            if (versionMatcher.find()) {
                String version = versionMatcher.group(1).trim();
                builder.version(version);
            } else {
                // Try alternative version patterns for Tomcat 10
                Pattern altVersionPattern = Pattern.compile("Apache Tomcat/([\\d.]+)");
                Matcher altVersionMatcher = altVersionPattern.matcher(result);
                if (altVersionMatcher.find()) {
                    builder.version("Apache Tomcat/" + altVersionMatcher.group(1));
                } else {
                    // Look for version in the specific format from your server
                    Pattern debianVersionPattern = Pattern.compile("Apache Tomcat/([\\d.]+) \\(Debian\\)");
                    Matcher debianVersionMatcher = debianVersionPattern.matcher(result);
                    if (debianVersionMatcher.find()) {
                        builder.version("Apache Tomcat/" + debianVersionMatcher.group(1) + " (Debian)");
                    }
                }
            }

            // Extract memory usage (improved parsing)
            Pattern memoryPattern = Pattern.compile("Memory:\\s*(\\d+)");
            Matcher memoryMatcher = memoryPattern.matcher(result);
            if (memoryMatcher.find()) {
                long memoryKB = Long.parseLong(memoryMatcher.group(1));
                builder.memoryUsage(formatMemoryUsage(memoryKB));
            } else {
                // Try alternative memory pattern
                Pattern altMemoryPattern = Pattern.compile("tomcat_memory.stdout:\\s*(\\d+)");
                Matcher altMemoryMatcher = altMemoryPattern.matcher(result);
                if (altMemoryMatcher.find()) {
                    long memoryKB = Long.parseLong(altMemoryMatcher.group(1));
                    builder.memoryUsage(formatMemoryUsage(memoryKB));
                }
            }

            // Extract port information (expanded for Tomcat 10)
            if (result.contains("tomcat_ports")) {
                builder.port("8080, 8443, 8005, 8009, 8006");
            }

            // Add installation path information
            Map<String, Object> metrics = new HashMap<>();
            if (result.contains("tomcat_paths")) {
                metrics.put("installationPaths", "Check tomcat_paths output for details");
            }
            if (result.contains("/usr/share/tomcat10")) {
                metrics.put("catalinaHome", "/usr/share/tomcat10");
            }
            if (result.contains("/var/lib/tomcat10")) {
                metrics.put("catalinaBase", "/var/lib/tomcat10");
            }
            if (!metrics.isEmpty()) {
                builder.metrics(metrics);
            }
        } else {
            builder.status("STOPPED");
            
            // Add installation path information even if stopped
            Map<String, Object> metrics = new HashMap<>();
            if (result.contains("tomcat_paths")) {
                metrics.put("installationPaths", "Check tomcat_paths output for details");
            }
            if (result.contains("/usr/share/tomcat10")) {
                metrics.put("catalinaHome", "/usr/share/tomcat10");
            }
            if (result.contains("/var/lib/tomcat10")) {
                metrics.put("catalinaBase", "/var/lib/tomcat10");
            }
            if (!metrics.isEmpty()) {
                builder.metrics(metrics);
            }
        }

        return builder.build();
    }

    private ServerStatus.ServiceStatus parseIISStatus(String result, ServerStatus.ServiceStatus.ServiceStatusBuilder builder) {
        builder.serviceName("IIS");

        // Check if IIS service is running
        if (result.contains("iis_service") && result.contains("state: started")) {
            builder.status("RUNNING");
            
            // Extract version
            Pattern versionPattern = Pattern.compile("VersionString\\s*:\\s*([^\\n]+)");
            Matcher versionMatcher = versionPattern.matcher(result);
            if (versionMatcher.find()) {
                builder.version(versionMatcher.group(1).trim());
            }
        } else {
            builder.status("STOPPED");
        }

        return builder.build();
    }

    private ServerStatus.ServiceStatus parseJBossStatus(String result, ServerStatus.ServiceStatus.ServiceStatusBuilder builder) {
        builder.serviceName("JBoss");

        // Check if JBoss process is running
        if (result.contains("jboss_process") && !result.contains("jboss_process.stdout_lines: []")) {
            builder.status("RUNNING");
            
            // Extract version
            Pattern versionPattern = Pattern.compile("JBoss.*?([\\d.]+)");
            Matcher versionMatcher = versionPattern.matcher(result);
            if (versionMatcher.find()) {
                builder.version(versionMatcher.group(1).trim());
            }

            // Extract memory usage
            Pattern memoryPattern = Pattern.compile("jboss_memory.stdout:\\s*(\\d+)");
            Matcher memoryMatcher = memoryPattern.matcher(result);
            if (memoryMatcher.find()) {
                long memoryKB = Long.parseLong(memoryMatcher.group(1));
                builder.memoryUsage(formatMemoryUsage(memoryKB));
            }

            // Extract port information
            if (result.contains("jboss_ports")) {
                builder.port("8080, 8443, 9990, 9999");
            }
        } else {
            builder.status("STOPPED");
        }

        return builder.build();
    }

    private String formatMemoryUsage(long memoryKB) {
        if (memoryKB > 1024 * 1024) {
            return String.format("%.2f GB", memoryKB / (1024.0 * 1024.0));
        } else if (memoryKB > 1024) {
            return String.format("%.2f MB", memoryKB / 1024.0);
        } else {
            return memoryKB + " KB";
        }
    }

    private String determineOverallStatus(List<ServerStatus.ServiceStatus> services) {
        if (services.isEmpty()) {
            return "ERROR";
        }

        boolean hasRunningService = services.stream()
            .anyMatch(service -> "RUNNING".equals(service.getStatus()));

        if (hasRunningService) {
            return "ONLINE";
        } else {
            return "OFFLINE";
        }
    }

    private ServerStatus createErrorStatus(AnsibleRequest request, String errorMessage) {
        return ServerStatus.builder()
            .serverName(request.getServerName())
            .ipAddress(request.getIpAddress())
            .status("ERROR")
            .lastChecked(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .additionalInfo(Map.of("error", errorMessage))
            .services(new ArrayList<>())
            .build();
    }
} 