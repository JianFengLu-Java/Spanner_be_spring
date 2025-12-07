package com.lujianfeng.spanner.dto;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@Component
@Setter
@Getter
public class UserDTO {
    private String userName;

    private String password;

    private String avatarUrl;

    private String gender;

    private String email;

    private String phone;

    private String address;

    private Long age;
}
