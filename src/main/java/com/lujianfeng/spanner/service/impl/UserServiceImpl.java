package com.lujianfeng.spanner.service.impl;

import com.lujianfeng.spanner.dto.UserDTO;
import com.lujianfeng.spanner.entity.UserEntity;
import com.lujianfeng.spanner.mapper.UserMapper;
import com.lujianfeng.spanner.repository.UserRepository;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;


    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.userMapper = userMapper;
    }

    @Override
    public UserVO register(UserDTO userDTO) {
        UserEntity userEntity = userMapper.toUserEntity(userDTO);
        userEntity.setPassword(bCryptPasswordEncoder.encode(userDTO.getPassword()));

        UserEntity user = userRepository.save(userEntity);
        return userMapper.toUserVO(user);
    }

    @Override
    public Map<String,Object> login(UserDTO userDTO) {
        String userName = userDTO.getUserName();
        String password = userDTO.getPassword();
        UserEntity user =  userRepository.findByUserName(userName);

        if(user == null){
            log.info("the user {} is not exist", userName);
            return Map.of(
                    "message","user is not exist",
                    "status",false
            );
        }
        boolean isPass =  bCryptPasswordEncoder.matches(userDTO.getPassword(), user.getPassword());
        if(isPass){
            log.info("the user {} login OK", userName);
            return Map.of(

                    "message","user login OK",
                    "status",true
            );
        }else {
            log.info("the user {} login FAIL", userName);
            return Map.of(
                    "message","user login FAIL",
                    "status",false
            );
        }


    }
}
