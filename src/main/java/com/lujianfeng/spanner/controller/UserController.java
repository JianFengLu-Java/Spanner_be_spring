package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.dto.UserLoginRequestDTO;
import com.lujianfeng.spanner.dto.UserRegisterRequestDTO;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.vo.UserVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 用户控制器
 * 获取前端发送的用户请求
 */


//注册为Rest请求
@RestController
//根路径
@RequestMapping("/user")


public class UserController {

    private final UserService userService;
    //构造方法注入Bean
    public UserController(UserService userService) {
        this.userService = userService;
    }

    //用户注册
    @PostMapping("/register")
    public ResponseEntity<Map<String,Object>> register(@RequestBody UserRegisterRequestDTO userRegisterRequestDTO) {
       try{
           UserVO userInfo = userService.register(userRegisterRequestDTO);
           return ResponseEntity.ok(
                   Map.of(
                           "code",200,
                           "data",userInfo,
                           "status","success"

                   )
           );
       }catch (Exception e){
           return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message","error"));
       }

    }

    //用户登录
    @PostMapping("/login")
    public  ResponseEntity<Map<String,Object>> login(@RequestBody UserLoginRequestDTO userLoginRequestDTO) {
        try{
            Map<String,Object> result = userService.login(userLoginRequestDTO);
            if (result.get("status").equals(true)) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.status(HttpStatus.FOUND).body(result);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message","error"));
        }



    }

    //获取用户信息
    @GetMapping("/me")
    public ResponseEntity<Map<String,Object>> me(){
        UserVO user = userService.getUserInfo();
        return ResponseEntity.ok(Map.of("user",user));
    }


}
