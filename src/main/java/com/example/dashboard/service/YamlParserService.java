package com.example.dashboard.service;

import com.example.dashboard.model.Application;
import com.example.dashboard.model.ApplicationList;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.TypeDescription;
import java.io.InputStream;
import java.util.List;

@Service
public class YamlParserService {
    
    public List<Application> parseYaml() {
        try {
            Constructor constructor = new Constructor(ApplicationList.class);
            TypeDescription applicationListType = new TypeDescription(ApplicationList.class);
            applicationListType.addPropertyParameters("applications", Application.class);
            constructor.addTypeDescription(applicationListType);
            
            Yaml yaml = new Yaml(constructor);
            InputStream inputStream = new ClassPathResource("example.yaml").getInputStream();
            ApplicationList applicationList = yaml.load(inputStream);
            return applicationList.getApplications();
        } catch (Exception e) {
            throw new RuntimeException("Error parsing YAML file", e);
        }
    }
} 