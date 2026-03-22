package com.pet.walkthroughserver.modules.user.service;

import com.pet.walkthroughserver.exceptions.AppException;
import com.pet.walkthroughserver.modules.user.dto.GitHubUserData;
import com.pet.walkthroughserver.modules.user.dto.UserResponse;
import com.pet.walkthroughserver.modules.user.entity.UserEntity;
import com.pet.walkthroughserver.modules.user.mapper.UserMapper;
import com.pet.walkthroughserver.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserEntity findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("User not found"));
    }

    @Override
    @Transactional
    public UserEntity findOrCreateByGitHub(GitHubUserData data) {
        return userRepository.findByGithubId(data.githubId())
                .map(existing -> {
                    existing.setUsername(data.username());
                    existing.setDisplayName(data.displayName());
                    existing.setEmail(data.email());
                    existing.setAvatarUrl(data.avatarUrl());
                    existing.setGithubAccessToken(data.accessToken());
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    UserEntity newUser = UserEntity.builder()
                            .githubId(data.githubId())
                            .username(data.username())
                            .displayName(data.displayName())
                            .email(data.email())
                            .avatarUrl(data.avatarUrl())
                            .githubAccessToken(data.accessToken())
                            .build();
                    return userRepository.save(newUser);
                });
    }

    @Override
    public UserResponse getCurrentUser(UUID userId) {
        UserEntity user = findById(userId);
        return userMapper.toResponse(user);
    }
}
