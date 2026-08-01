package com.example.careerpilot.knowledge;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "careerpilot.knowledge")
public class KnowledgeProperties {

    private String path =
            "classpath:knowledge/*.md";
}