package com.example.dashboard.model;

import lombok.Data;

@Data
public class Service {
    private String name;
    private String type;
    private String cmd;
    private String startscript;
    private String dbtype;
    private String tnsalias;
} 