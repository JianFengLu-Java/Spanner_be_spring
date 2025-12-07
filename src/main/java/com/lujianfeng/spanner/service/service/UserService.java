package com.lujianfeng.spanner.service.service;

import com.lujianfeng.spanner.dto.UserDTO;
import com.lujianfeng.spanner.vo.UserVO;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface UserService {
    UserVO register(UserDTO userDTO);
    Map<String,Object> login(UserDTO userDTO);

}
