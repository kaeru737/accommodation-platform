package com.rs.acd.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Email(message = "올바른 이메일 형식을 입력하세요.")
    @NotBlank(message = "이메일은 필수입니다.")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Column(nullable = false)
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    private String profileImage;

    private LocalDateTime createdAt;

    @NotBlank(message = "역할은 필수입니다.")
    @Column(nullable = false)
    private String role;

    // ✅ getId() 추가: user.getId() 형태로 접근 가능하도록
    public Long getId() {
        return this.userId;
    }
}
