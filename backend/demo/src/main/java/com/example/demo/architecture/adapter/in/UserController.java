package com.example.demo.architecture.adapter.in;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.architecture.adapter.in.dto.UserResponse;
import com.example.demo.architecture.application.GetUserUseCase;
import com.example.demo.architecture.domain.User;

@RestController
public class UserController {

    private final GetUserUseCase getUserUseCase;
    
    public UserController(GetUserUseCase getUserUseCase) {
        this.getUserUseCase = getUserUseCase;
    }
    @GetMapping("/users/{id}")
    public UserResponse getUser(@PathVariable Long id) {

        User user = getUserUseCase.execute(id);

        return new UserResponse(
                user.getId(),
                user.getName()
        );
    }

    @GetMapping("/users/new/{id}")
    public UserResponse saveUser(@PathVariable Long id) {
        // getUserUseCase

        User user = getUserUseCase.Save(id);
        return new UserResponse(
                user.getId(),
                user.getName()
        );
    }
    
}