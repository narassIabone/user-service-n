package ru.cinimex.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.cinimex.userservice.domain.TempCodeEntity;
import ru.cinimex.userservice.domain.UserEntity;
import ru.cinimex.userservice.dto.*;
import ru.cinimex.userservice.exception.InvalidCodeException;
import ru.cinimex.userservice.exception.UserAlreadyExistsException;
import ru.cinimex.userservice.exception.UserNotFoundException;
import ru.cinimex.userservice.mapper.UserMapper;
import ru.cinimex.userservice.repository.TempCodeRepository;
import ru.cinimex.userservice.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TempCodeRepository tempCodeRepository;
    private final UserMapper userMapper;
    private final KafkaTemplate<String, NotificationDto> kafkaTemplate;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TransactionTemplate transactionTemplate;

    public UUID register(UserRegisterDto dto) {
        var result = transactionTemplate.execute(status -> {
            if (userRepository.existsByUsernameOrEmail(dto.getUsername(), dto.getEmail())) {
                throw new UserAlreadyExistsException("Пользователь с таким логином или почтой уже существует");
            }

            UserEntity user = userMapper.toEntity(dto);
            userRepository.save(user);

            String code = String.format("%06d", new Random().nextInt(1000000));
            TempCodeEntity tempCode = TempCodeEntity.builder()
                    .userId(user.getId())
                    .code(code)
                    .build();
            tempCodeRepository.save(tempCode);

            // Возвращаем данные, которые понадобятся для Kafka
            return new RegistrationContext(user.getId(), user.getEmail(), code);
        });

        if (result != null) {
            NotificationDto notification = NotificationDto.builder()
                    .email(result.email())
                    .header("Подтверждение почты")
                    .body("Ваш код подтверждения - " + result.code())
                    .build();

            //kafkaTemplate.send("notification.message.in", notification);
            return result.userId();
        }

        throw new RuntimeException("Ошибка при регистрации: транзакция не вернула результат");
    }

    // Вспомогательный record (или внутренний класс), чтобы передать данные из транзакции в Kafka
    private record RegistrationContext(UUID userId, String email, String code) {}

    @Transactional
    public void confirmRegistration(CodeConfirmDto dto) {
        UserEntity user = userRepository.findById(dto.getId())
                .orElseThrow(() -> new UserNotFoundException("Пользователь с таким id не существует"));

        if (user.isActive()) {
            throw new InvalidCodeException("Пользователь уже активирован");
        }

        TempCodeEntity tempCode = tempCodeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new InvalidCodeException("Код подтверждения не найден или истек"));

        if (!tempCode.getCode().equals(dto.getCode())) {
            throw new InvalidCodeException("Неверный код подтверждения");
        }

        user.setActive(true);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        tempCodeRepository.delete(tempCode);
    }

    public String login(LoginRequestDto dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
        );

        return jwtService.generateToken(authentication);
    }
}