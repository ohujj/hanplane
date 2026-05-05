package com.hanplane.domain.auth.repository;

import com.hanplane.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<RefreshToken, Long> {



    Optional<RefreshToken> findByUserId(Long userId);

    Optional<RefreshToken> findByToken(String refreshToken);
}
