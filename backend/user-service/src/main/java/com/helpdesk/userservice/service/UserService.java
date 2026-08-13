package com.helpdesk.userservice.service;

import com.helpdesk.userservice.dto.UpdateUserRequest;
import com.helpdesk.userservice.dto.UserResponse;
import com.helpdesk.userservice.entity.Role;
import com.helpdesk.userservice.entity.User;
import com.helpdesk.userservice.exception.BadRequestException;
import com.helpdesk.userservice.exception.ResourceNotFoundException;
import com.helpdesk.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getUserById(UUID id) {
        return UserResponse.fromEntity(findUserOrThrow(id));
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    public List<UserResponse> getAgents() {
        return userRepository.findByRole(Role.AGENT).stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request, UUID requesterId) {
        User user = findUserOrThrow(id);
        boolean isSelf = user.getId().equals(requesterId);

        if (isSelf && request.getRole() != null
                && user.getRole() == Role.ADMIN
                && request.getRole() != Role.ADMIN) {
            throw new BadRequestException("Admins cannot remove their own admin access");
        }

        if (isSelf && request.getActive() != null && !request.getActive()) {
            throw new BadRequestException("Admins cannot deactivate their own account");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        return UserResponse.fromEntity(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(UUID id, UUID requesterId) {
        if (id.equals(requesterId)) {
            throw new BadRequestException("Admins cannot delete their own account");
        }
        User user = findUserOrThrow(id);
        userRepository.delete(user);
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
