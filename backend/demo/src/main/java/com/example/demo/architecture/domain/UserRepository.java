package com.example.demo.architecture.domain;

import java.util.Optional;

public interface UserRepository{
    Optional<User> findById(Long id);

    User save(User user);
}