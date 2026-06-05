package com.example.demo.architecture.application;

import org.springframework.stereotype.Service;

import com.example.demo.architecture.domain.User;
import com.example.demo.architecture.domain.UserRepository;

@Service
public class GetUserService implements GetUserUseCase{

    private final UserRepository userRepository;

    public GetUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public User execute(Long id) {
        return userRepository.findById(id)
                .orElseThrow();
    }

    @Override
    public User Save(Long id) {
        return userRepository.save(new User(id, "New User : " + id));
    }
}