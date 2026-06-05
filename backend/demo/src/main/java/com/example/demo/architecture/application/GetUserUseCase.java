package com.example.demo.architecture.application;

import com.example.demo.architecture.domain.User;

public interface GetUserUseCase {

    public User execute(Long id);

    public User Save(Long id);
}