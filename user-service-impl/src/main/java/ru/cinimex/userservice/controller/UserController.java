package ru.cinimex.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.cinimex.userservice.dto.UserResponseDto;
import ru.cinimex.userservice.service.UserService;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController implements UserControllerApi {

    private final UserService userService;

    @Override
    public ResponseEntity<UserResponseDto> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @Override
    public ResponseEntity<UserResponseDto> getUserByLogin(String login) {
        return ResponseEntity.ok(userService.getUserByLogin(login));
    }

    @Override
    public ResponseEntity<String> generateTechToken(OffsetDateTime expiredDate) {
        return ResponseEntity.ok(userService.generateTechToken(expiredDate));
    }
}