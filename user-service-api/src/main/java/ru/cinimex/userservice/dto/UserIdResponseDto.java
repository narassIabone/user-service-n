package ru.cinimex.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserIdResponseDto {
    private UUID id;
}