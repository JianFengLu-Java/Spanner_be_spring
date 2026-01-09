package com.lujianfeng.spanner.dto.user;


import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

/**
 * @author mac
 */
@Getter
@Setter
@Component
public class UserLoginRequestDTO {
    private String account;
    private String password;
}
