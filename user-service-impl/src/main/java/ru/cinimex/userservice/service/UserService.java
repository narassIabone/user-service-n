package ru.cinimex.userservice.service;

import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import ru.cinimex.userservice.domain.UserEntity;
import ru.cinimex.userservice.dto.UserResponseDto;
import ru.cinimex.userservice.mapper.UserMapper;
import ru.cinimex.userservice.repository.UserRepository;
import ru.cinimex.userservice.exception.UserNotFoundException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    public UserResponseDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String login = authentication.getName();

        return getUserByLogin(login);
    }

    public UserResponseDto getUserByLogin(String login) {
        UserEntity user = userRepository.findByUsername(login)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с логином " + login + " не найден"));
        return userMapper.toResponseDto(user);
    }

    public String generateTechToken(OffsetDateTime expiredDate) {
        return jwtService.generateTechToken(expiredDate);
    }
}
