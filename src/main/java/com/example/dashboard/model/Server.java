package com.example.dashboard.model;

import lombok.Data;
import java.util.List;

@Data
public class Server {
    private String name;
    private String ip;
    private String os;
    private List<Service> services;
} 