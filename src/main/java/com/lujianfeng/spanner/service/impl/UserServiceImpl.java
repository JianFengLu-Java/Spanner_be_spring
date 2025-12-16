package com.lujianfeng.spanner.service.impl;

import com.lujianfeng.spanner.dto.user.UserLoginRequestDTO;
import com.lujianfeng.spanner.dto.user.UserRegisterRequestDTO;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.mapper.UserMapper;
import com.lujianfeng.spanner.repository.UserRepository;
import com.lujianfeng.spanner.security.SecurityUser;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.util.JwtUtil;
import com.lujianfeng.spanner.vo.UserVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtUtil jwtUtil;


    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, BCryptPasswordEncoder bCryptPasswordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public UserVO register(UserRegisterRequestDTO userRegisterRequestDTO) {
        //UserEntity是用户实体类，保存了所有的用户信息
        UserEntity userEntity = userMapper.toUserEntity(userRegisterRequestDTO);
        userEntity.setPassword(bCryptPasswordEncoder.encode(userRegisterRequestDTO.getPassword()));
        UserEntity user = userRepository.save(userEntity);
        return userMapper.toUserVO(user);
    }

    @Override
    public UserVO getUserInfo() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }

        SecurityUser securityUser = (SecurityUser) auth.getPrincipal();
        assert securityUser != null;
        UserEntity user = securityUser.userEntity();
        return userMapper.toUserVO(user);
    }

    @Override
    public Map<String, Object> login(UserLoginRequestDTO userLoginRequestDTO) {
        String userName = userLoginRequestDTO.getUserName();
        System.out.println(userName);
        UserEntity user = userRepository.findByUserName(userName);
        if (user == null) {
            return Map.of(
                    "message", "用户不存在",
                    "status", false,
                    "token", "null"
            );
        }
        boolean isPass = bCryptPasswordEncoder.matches(userLoginRequestDTO.getPassword(), user.getPassword());
        if (isPass) {

            String token = jwtUtil.generateToken(userName);
            return Map.of(
                    "message", "登录成功，保存Token",
                    "status", true,
                    "token", token
            );
        } else {
            return Map.of(
                    "message", "user login FAIL",
                    "status", false,
                    "token", "null"
            );
        }


    }
}
