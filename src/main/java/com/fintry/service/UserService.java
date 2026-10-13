package com.fintry.service;

import com.fintry.entity.Role;
import com.fintry.entity.User;
import com.fintry.repository.UserRepository;
import org.springframework.stereotype.Service;
import com.fintry.dto.CreateUserRequest;
import com.fintry.dto.UserResponse;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @org.springframework.security.access.prepost.PreAuthorize("denyAll()")
    public UserResponse createUser(CreateUserRequest request) {

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }
    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}