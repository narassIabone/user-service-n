package ru.cinimex.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.cinimex.userservice.domain.UserEntity;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUsername(String username);
    boolean existsByUsernameOrEmail(String username, String email);
}