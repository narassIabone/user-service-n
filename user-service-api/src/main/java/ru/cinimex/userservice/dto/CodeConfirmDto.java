package ru.cinimex.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.UUID;

@Data
public class CodeConfirmDto {
    @NotNull
    private UUID id;

    @NotBlank
    @Size(min = 6, max = 6)
    private String code;
}
