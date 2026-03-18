package com.pet.walkthroughserver.modules.user.service;

import com.pet.walkthroughserver.exceptions.AppException;
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
    public UserEntity findOrCreateByGitHub(Long githubId, String username, String displayName,
                                            String email, String avatarUrl, String accessToken) {
        return userRepository.findByGithubId(githubId)
                .map(existing -> {
                    existing.setUsername(username);
                    existing.setDisplayName(displayName);
                    existing.setEmail(email);
                    existing.setAvatarUrl(avatarUrl);
                    existing.setGithubAccessToken(accessToken);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    UserEntity newUser = UserEntity.builder()
                            .githubId(githubId)
                            .username(username)
                            .displayName(displayName)
                            .email(email)
                            .avatarUrl(avatarUrl)
                            .githubAccessToken(accessToken)
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
