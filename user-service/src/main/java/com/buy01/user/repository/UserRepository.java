package com.buy01.user.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.buy01.user.model.User;

public interface UserRepository extends MongoRepository<User, String> {
 
    Boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}

