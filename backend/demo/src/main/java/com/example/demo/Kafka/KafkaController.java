package com.example.demo.Kafka;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/Kafka")
public class KafkaController {
    
    @GetMapping("/get")
    public String MessageGet() {
        return "Test";
    }
    
}
