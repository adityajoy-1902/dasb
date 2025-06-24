#!/bin/bash

# Test script for Ansible Service API
# Make sure the Dashboard application is running on localhost:8080

BASE_URL="http://localhost:8080/api/ansible"

echo "=== Ansible Service API Test ==="
echo

# Test 1: Health Check
echo "1. Testing Health Check..."
curl -s "$BASE_URL/health" | jq .
echo

# Test 2: Get Supported Services
echo "2. Getting Supported Services..."
curl -s "$BASE_URL/supported-services" | jq .
echo

# Test 3: Check Single Server Status (Example with localhost)
echo "3. Testing Single Server Status Check..."
curl -s -X POST "$BASE_URL/check-status" \
  -H "Content-Type: application/json" \
  -d '{
    "serverName": "test-server",
    "ipAddress": "127.0.0.1",
    "username": "testuser",
    "password": "testpass",
    "servicesToCheck": ["TOMCAT"],
    "useSudo": false,
    "timeout": 30
  }' | jq .
echo

# Test 4: Check Multiple Servers
echo "4. Testing Multiple Server Status Check..."
curl -s -X POST "$BASE_URL/check-multiple" \
  -H "Content-Type: application/json" \
  -d '[
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
      "servicesToCheck": ["JBOSS"],
      "useSudo": false,
      "timeout": 60
    }
  ]' | jq .
echo

echo "=== Test Complete ==="
echo "Note: Some tests may fail if Ansible is not installed or target servers are not accessible."
echo "This is expected behavior for demonstration purposes." 