# Ansible Server Status Checker Service

This service provides a REST API to check the status of various server services (Tomcat, IIS, JBoss) using Ansible playbooks.

## Features

- **Multi-Service Support**: Check status of Tomcat, IIS, and JBoss servers
- **Real-time Monitoring**: Get live status, version, memory usage, and port information
- **REST API**: Easy integration with web applications
- **Ansible Integration**: Uses Ansible for reliable server connectivity
- **Error Handling**: Comprehensive error handling and logging

## Prerequisites

1. **Ansible Installation**: Ensure Ansible is installed on the system
   ```bash
   # Ubuntu/Debian
   sudo apt-get install ansible
   
   # CentOS/RHEL
   sudo yum install ansible
   
   # macOS
   brew install ansible
   ```

2. **SSH Access**: Ensure SSH access to target servers
3. **Java 11+**: Required for the Spring Boot application

## API Endpoints

### 1. Check Single Server Status
```http
POST /api/ansible/check-status
Content-Type: application/json

{
  "serverName": "web-server-01",
  "ipAddress": "192.168.1.100",
  "username": "admin",
  "password": "password123",
  "servicesToCheck": ["TOMCAT", "JBOSS"],
  "useSudo": true,
  "timeout": 60
}
```

### 2. Check Multiple Servers
```http
POST /api/ansible/check-multiple
Content-Type: application/json

[
  {
    "serverName": "web-server-01",
    "ipAddress": "192.168.1.100",
    "username": "admin",
    "password": "password123",
    "servicesToCheck": ["TOMCAT"],
    "useSudo": true,
    "timeout": 60
  },
  {
    "serverName": "app-server-02",
    "ipAddress": "192.168.1.101",
    "username": "admin",
    "sshKeyPath": "/path/to/private/key",
    "servicesToCheck": ["JBOSS", "IIS"],
    "useSudo": false,
    "timeout": 60
  }
]
```

### 3. Health Check
```http
GET /api/ansible/health
```

### 4. Get Supported Services
```http
GET /api/ansible/supported-services
```

## Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| serverName | String | Yes | Name/identifier for the server |
| ipAddress | String | Yes | IP address of the target server |
| username | String | Yes | SSH username |
| password | String | No* | SSH password (if not using SSH key) |
| sshKeyPath | String | No* | Path to SSH private key (if not using password) |
| servicesToCheck | List<String> | Yes | List of services to check (TOMCAT, IIS, JBOSS) |
| useSudo | Boolean | No | Whether to use sudo for commands (default: false) |
| timeout | Integer | No | Timeout in seconds (default: 60) |

*Either password or sshKeyPath must be provided

## Response Format

```json
{
  "serverName": "web-server-01",
  "ipAddress": "192.168.1.100",
  "os": "Linux",
  "status": "ONLINE",
  "lastChecked": "2024-01-15T10:30:00",
  "services": [
    {
      "serviceName": "Tomcat",
      "serviceType": "TOMCAT",
      "status": "RUNNING",
      "port": "8080, 8443, 8005",
      "version": "Apache Tomcat/9.0.65",
      "uptime": "2 days, 5 hours",
      "memoryUsage": "512.45 MB",
      "cpuUsage": "2.3%",
      "lastRestart": "2024-01-13T05:15:00",
      "metrics": {
        "heapUsed": "256MB",
        "heapMax": "1GB",
        "threadCount": 45
      }
    }
  ],
  "additionalInfo": {
    "ansibleVersion": "2.12.0",
    "executionTime": "2.3s"
  }
}
```

## Service Status Values

- **Server Status**: `ONLINE`, `OFFLINE`, `ERROR`
- **Service Status**: `RUNNING`, `STOPPED`, `ERROR`

## Example Usage

### Using curl

```bash
# Check Tomcat status
curl -X POST http://localhost:8080/api/ansible/check-status \
  -H "Content-Type: application/json" \
  -d '{
    "serverName": "prod-web-01",
    "ipAddress": "10.0.1.100",
    "username": "deploy",
    "password": "securepass",
    "servicesToCheck": ["TOMCAT"],
    "useSudo": true,
    "timeout": 30
  }'

# Check multiple services
curl -X POST http://localhost:8080/api/ansible/check-status \
  -H "Content-Type: application/json" \
  -d '{
    "serverName": "app-server-01",
    "ipAddress": "10.0.1.101",
    "username": "admin",
    "sshKeyPath": "/home/user/.ssh/id_rsa",
    "servicesToCheck": ["TOMCAT", "JBOSS"],
    "useSudo": false,
    "timeout": 60
  }'
```

### Using JavaScript/Fetch

```javascript
const checkServerStatus = async (serverConfig) => {
  try {
    const response = await fetch('/api/ansible/check-status', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(serverConfig)
    });
    
    const status = await response.json();
    console.log('Server Status:', status);
    return status;
  } catch (error) {
    console.error('Error checking server status:', error);
  }
};

// Usage
checkServerStatus({
  serverName: 'web-server-01',
  ipAddress: '192.168.1.100',
  username: 'admin',
  password: 'password123',
  servicesToCheck: ['TOMCAT', 'JBOSS'],
  useSudo: true,
  timeout: 60
});
```

## Security Considerations

1. **SSH Keys**: Prefer SSH key authentication over passwords
2. **Network Security**: Ensure secure network connectivity to target servers
3. **Credentials**: Store credentials securely (consider using environment variables or secure vaults)
4. **Firewall**: Ensure target servers allow SSH connections from the application server

## Troubleshooting

### Common Issues

1. **SSH Connection Failed**
   - Verify SSH credentials
   - Check network connectivity
   - Ensure SSH service is running on target server

2. **Permission Denied**
   - Use `useSudo: true` if commands require elevated privileges
   - Verify user has necessary permissions

3. **Service Not Found**
   - Ensure the service is installed on the target server
   - Check service installation paths

4. **Timeout Issues**
   - Increase timeout value for slow networks
   - Check server responsiveness

### Debug Mode

Enable debug logging by setting the log level in `application.properties`:

```properties
logging.level.com.example.dashboard.service.AnsibleService=DEBUG
```

## Integration with Dashboard

This service integrates seamlessly with the existing Dashboard application. You can:

1. Add server monitoring to the existing application tabs
2. Create new monitoring dashboards
3. Set up automated status checks
4. Configure alerts based on service status

## Performance Optimization

- Use SSH key authentication for faster connections
- Implement connection pooling for multiple servers
- Cache results for frequently checked servers
- Use parallel execution for multiple server checks

## Contributing

To add support for new services:

1. Add service type to the `AnsibleService` class
2. Implement service-specific tasks in the playbook generation
3. Add parsing logic for the new service
4. Update the API documentation

## License

This service is part of the Dashboard application and follows the same licensing terms. 