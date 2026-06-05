package com.example.demo.architecture.adapter.out.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "users")
public class UserDocument {

    @Id
    private Long id;

    private String name;

}