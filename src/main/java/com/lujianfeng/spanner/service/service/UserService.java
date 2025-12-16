package com.lujianfeng.spanner.service.service;

import com.lujianfeng.spanner.dto.user.UserLoginRequestDTO;
import com.lujianfeng.spanner.dto.user.UserRegisterRequestDTO;
import com.lujianfeng.spanner.vo.user.LoginVO;
import com.lujianfeng.spanner.vo.user.UserVO;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    UserVO register(UserRegisterRequestDTO userRegisterRequestDTO);

    LoginVO login(UserLoginRequestDTO userLoginRequestDTO);

    UserVO getUserInfo();

}
