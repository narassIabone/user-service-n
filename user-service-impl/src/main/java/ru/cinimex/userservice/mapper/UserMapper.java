package ru.cinimex.userservice.mapper;

import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.cinimex.userservice.domain.UserEntity;
import ru.cinimex.userservice.domain.UserRole;
import ru.cinimex.userservice.dto.UserRegisterDto;
import ru.cinimex.userservice.dto.UserResponseDto;

import java.time.OffsetDateTime;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @Builder(disableBuilder = true),
        imports = {UserRole.class, OffsetDateTime.class}
)
public abstract class UserMapper {

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", constant = "USER")
    @Mapping(target = "active", constant = "false")

    public abstract UserEntity toEntity(UserRegisterDto dto);
    public abstract UserResponseDto toResponseDto(UserEntity entity);

    @AfterMapping
    protected void fillComplexFields(UserRegisterDto dto, @MappingTarget UserEntity entity) {
        // Хешируем пароль
        if (dto.getPassword() != null) {
            entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        OffsetDateTime now = OffsetDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
    }
}