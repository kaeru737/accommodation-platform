package com.rs.acd.service;

import com.rs.acd.domain.User;
import com.rs.acd.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User save(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalStateException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByNickname(user.getNickname())) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }

        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // ✅ 마이페이지에서 사용
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
