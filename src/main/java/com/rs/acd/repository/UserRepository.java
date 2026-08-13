package com.rs.acd.repository;

import com.rs.acd.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository // 스프링이 이 인터페이스를 Repository(DAO)로 인식하게 해줌
public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일을 기준으로 사용자 1명을 찾는 메서드 (자동 쿼리 생성됨)
    User findByEmail(String email);

    // 이메일 중복 확인
    boolean existsByEmail(String email);

    // 닉네임 중복 확인
    boolean existsByNickname(String nickname);
}
