package com.example.demo.architecture.adapter.out.mongodb;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.demo.architecture.domain.User;
import com.example.demo.architecture.domain.UserRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MongoUserRepositoryAdapter implements UserRepository {

    private final UserMongoRepository mongoRepository;

    @Override
    public Optional<User> findById(Long id) {
        return mongoRepository.findById(id).map(this::toDomain);
    }

    @Override
    public User save(User user) {

        UserDocument document = toDocument(user);

        UserDocument saved =
                mongoRepository.save(document);

        return toDomain(saved);
    }

    private User toDomain(UserDocument document) {
        return new User(
                document.getId(),
                document.getName()
        );
    }

    private UserDocument toDocument(
            User user
    ) {
        UserDocument document =
                new UserDocument();

        if(user.getId() != null) {
            document.setId(user.getId());
        }

        document.setName(user.getName());

        return document;
    }
}