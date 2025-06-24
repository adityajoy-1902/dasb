# Dashboard - Server Status Monitoring Application

A Spring Boot application that provides a web-based dashboard for monitoring server status using Ansible playbooks. The application can check the status of various services like Tomcat, IIS, and JBoss running on remote servers.

## Features

- 🌐 **Web Dashboard** - Modern web interface for server monitoring
- 🔧 **Ansible Integration** - Uses Ansible playbooks for server status checks
- 📊 **Multi-Service Support** - Monitor Tomcat, IIS, and JBoss services
- 🔐 **SSH Authentication** - Supports both password and SSH key authentication
- 📈 **Real-time Status** - Get current service status, versions, and metrics
- 🚀 **RESTful API** - Full API for integration with other systems

## Prerequisites

Before running this application, ensure you have the following installed:

### Required Software
- **Java 17** or higher
- **Maven** (or use the included Maven wrapper)
- **Ansible** (version 2.9 or higher)
- **sshpass** (for password-based SSH authentication)

### Installation Commands

#### macOS (using Homebrew)
```bash
# Install Java
brew install openjdk@17

# Install Ansible
brew install ansible

# Install sshpass
brew install sshpass
```

#### Ubuntu/Debian
```bash
# Install Java
sudo apt update
sudo apt install openjdk-17-jdk

# Install Ansible
sudo apt install ansible

# Install sshpass
sudo apt install sshpass
```

#### CentOS/RHEL
```bash
# Install Java
sudo yum install java-17-openjdk-devel

# Install Ansible
sudo yum install ansible

# Install sshpass
sudo yum install sshpass
```

## Quick Start

### 1. Clone the Repository
```bash
git clone <repository-url>
cd Dashboard
```

### 2. Build the Application
```bash
# Using Maven wrapper (recommended)
./mvnw clean install

# Or using system Maven
mvn clean install
```

### 3. Run the Application
```bash
# Using Maven wrapper
./mvnw spring-boot:run

# Or using system Maven
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Verify Installation
```bash
# Check if the application is running
curl http://localhost:8080/api/ansible/health
```

You should see:
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "service": "Ansible Status Checker"
}
```

## Configuration

### Server Configuration

The application uses Ansible playbooks to check server status. You need to configure your target servers in the inventory file.

#### 1. Update Inventory File
Edit `ansible-playbooks/inventory.ini`:

```ini
[servers]
your-server-name ansible_host=YOUR_SERVER_IP ansible_user=YOUR_USERNAME ansible_password=YOUR_PASSWORD

[all:vars]
ansible_connection=ssh
ansible_ssh_common_args='-o StrictHostKeyChecking=no'
```

#### 2. SSH Key Authentication (Alternative)
If you prefer SSH key authentication, update the inventory:

```ini
[servers]
your-server-name ansible_host=YOUR_SERVER_IP ansible_user=YOUR_USERNAME ansible_ssh_private_key_file=/path/to/your/private/key

[all:vars]
ansible_connection=ssh
ansible_ssh_common_args='-o StrictHostKeyChecking=no'
```

### Target Server Requirements

Your target servers should have:
- SSH access enabled
- Password authentication enabled (or SSH keys configured)
- The services you want to monitor (Tomcat, IIS, JBoss) installed

## Usage

### Web Dashboard

1. Open your browser and navigate to `http://localhost:8080`
2. Use the web interface to check server status

### REST API

The application provides a RESTful API for server status monitoring.

#### Check Server Status

**Endpoint:** `POST /api/ansible/check-status`

**Request Body:**
```json
{
  "serverName": "your-server-name",
  "ipAddress": "YOUR_SERVER_IP",
  "username": "YOUR_USERNAME",
  "password": "YOUR_PASSWORD",
  "servicesToCheck": ["TOMCAT", "JBOSS", "IIS"],
  "timeout": 60
}
```

**Example using curl:**
```bash
curl -X POST http://localhost:8080/api/ansible/check-status \
  -H "Content-Type: application/json" \
  -d '{
    "serverName": "web-server",
    "ipAddress": "34.131.147.204",
    "username": "aditya",
    "password": "aditya123",
    "servicesToCheck": ["TOMCAT"],
    "timeout": 60
  }'
```

**Response:**
```json
{
  "serverName": "web-server",
  "ipAddress": "34.131.147.204",
  "status": "ONLINE",
  "lastChecked": "2025-06-24T07:21:27.394667",
  "services": [
    {
      "serviceName": "Tomcat",
      "serviceType": "TOMCAT",
      "status": "RUNNING",
      "version": "Apache Tomcat/10.1.34 (Debian)",
      "memoryUsage": "264.04 MB",
      "metrics": {
        "catalinaHome": "/usr/share/tomcat10",
        "catalinaBase": "/var/lib/tomcat10"
      }
    }
  ]
}
```

#### Health Check

**Endpoint:** `GET /api/ansible/health`

**Example:**
```bash
curl http://localhost:8080/api/ansible/health
```

### Supported Services

#### Tomcat
- **Detection:** Process monitoring, version checking, port verification
- **Information:** Version, memory usage, installation paths, listening ports
- **Supported Versions:** Tomcat 8, 9, 10
- **Common Paths:** `/usr/share/tomcat*`, `/opt/tomcat`, `/data/tomcat`

#### JBoss
- **Detection:** Process monitoring, version checking
- **Information:** Version, memory usage, listening ports
- **Supported Versions:** JBoss 7, WildFly

#### IIS (Windows)
- **Detection:** Windows service monitoring
- **Information:** Version, application pools status
- **Requirements:** Windows target server with PowerShell access

## Project Structure

```
Dashboard/
├── src/
│   └── main/
│       ├── java/com/example/dashboard/
│       │   ├── controller/
│       │   │   ├── AnsibleController.java    # REST API endpoints
│       │   │   └── DashboardController.java  # Web dashboard
│       │   ├── model/
│       │   │   ├── AnsibleRequest.java       # Request models
│       │   │   ├── ServerStatus.java         # Response models
│       │   │   └── ...
│       │   └── service/
│       │       ├── AnsibleService.java       # Core Ansible integration
│       │       └── YamlParserService.java    # YAML parsing utilities
│       └── resources/
│           └── templates/
│               └── dashboard.html            # Web dashboard template
├── ansible-playbooks/
│   ├── ansible.cfg                          # Ansible configuration
│   ├── inventory.ini                        # Server inventory
│   └── status_check.yml                     # Status check playbook
├── pom.xml                                  # Maven dependencies
└── README.md                               # This file
```

## Troubleshooting

### Common Issues

#### 1. Connection Timeout
```
Failed to connect to the host via ssh: Operation timed out
```
**Solution:** Check if the target server is reachable and SSH is enabled.

#### 2. Authentication Failed
```
Permission denied (publickey)
```
**Solution:** Enable password authentication on the target server or use SSH keys.

#### 3. Ansible Not Found
```
ansible: command not found
```
**Solution:** Install Ansible using the package manager for your OS.

#### 4. sshpass Not Found
```
sshpass: command not found
```
**Solution:** Install sshpass for password-based SSH authentication.

#### 5. Java Version Issues
```
Unsupported major.minor version
```
**Solution:** Ensure Java 17 or higher is installed and set as default.

### Debug Mode

To run the application with debug logging:

```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dlogging.level.com.example.dashboard=DEBUG"
```

### Manual Ansible Testing

Test Ansible connectivity manually:

```bash
cd ansible-playbooks
ansible-playbook -i inventory.ini status_check.yml --verbose
```

## Development

### Adding New Services

To add support for a new service:

1. **Update the Ansible playbook** (`ansible-playbooks/status_check.yml`)
2. **Add parsing logic** in `AnsibleService.java`
3. **Update the service enum** in the request model
4. **Add UI components** in the dashboard template

### Building for Production

```bash
# Create JAR file
./mvnw clean package

# Run the JAR
java -jar target/dashboard-0.0.1-SNAPSHOT.jar
```

## Security Considerations

- **Passwords:** Store passwords securely, consider using environment variables
- **SSH Keys:** Use SSH key authentication when possible
- **Network:** Ensure proper firewall rules for SSH access
- **Updates:** Keep Ansible and dependencies updated

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions:
1. Check the troubleshooting section
2. Review the logs for error messages
3. Test Ansible connectivity manually
4. Open an issue on the repository

---

**Happy Monitoring! 🚀** 