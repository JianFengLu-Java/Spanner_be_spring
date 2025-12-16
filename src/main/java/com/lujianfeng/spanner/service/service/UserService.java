package com.lujianfeng.spanner.service.service;

import com.lujianfeng.spanner.dto.user.UserLoginRequestDTO;
import com.lujianfeng.spanner.dto.user.UserRegisterRequestDTO;
import com.lujianfeng.spanner.vo.UserVO;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface UserService {
    UserVO register(UserRegisterRequestDTO userRegisterRequestDTO);

    Map<String, Object> login(UserLoginRequestDTO userLoginRequestDTO);

    UserVO getUserInfo();

}
