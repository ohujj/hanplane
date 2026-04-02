package com.hanplane;

import com.hanplane.domain.user.repository.UserRepository;
import com.hanplane.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void 유저_저장_테스트() {
        // given
        User user = User.builder()
                .email("test@test.com")
                .password("1234")
                .build();

        // when
        User savedUser = userRepository.save(user);

        // then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("test@test.com");
    }
}