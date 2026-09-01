package com.eatrading.backend.service;

import com.eatrading.backend.entity.User;
import com.eatrading.backend.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Get user by UUID
     */
    public Optional<User> getUserByUuid(String uuid) {
        return userRepository.findByUserId(java.util.UUID.fromString(uuid));
    }

    /**
     * Create a new user
     */
    public User createUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Update an existing user
     */
    public User updateUser(Long id, User user) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        user.setId(id);
        return userRepository.save(user);
    }

    /**
     * Delete a user by ID
     */
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

}

