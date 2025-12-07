package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.dto.UserDTO;
import com.lujianfeng.spanner.entity.UserEntity;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.vo.UserVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 *用户控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String,Object>> register(@RequestBody UserDTO userDTO) {
       try{
           UserVO userInfo = userService.register(userDTO);
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

    @PostMapping("/login")
    public  ResponseEntity<Map<String,Object>> login(@RequestBody UserDTO userDTO) {
        try{
            Map<String,Object> result = userService.login(userDTO);
            if (result.get("status").equals(true)) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.status(HttpStatus.FOUND).body(result);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message","error"));
        }



    }


}
