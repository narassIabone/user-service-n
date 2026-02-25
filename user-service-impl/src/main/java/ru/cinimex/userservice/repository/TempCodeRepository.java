package ru.cinimex.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.cinimex.userservice.domain.TempCodeEntity;
import java.util.Optional;
import java.util.UUID;

public interface TempCodeRepository extends JpaRepository<TempCodeEntity, UUID> {
    Optional<TempCodeEntity> findByUserId(UUID userId);
}