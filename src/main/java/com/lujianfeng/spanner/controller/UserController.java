package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.dto.user.UserLoginRequestDTO;
import com.lujianfeng.spanner.dto.user.UserRegisterRequestDTO;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.vo.UserVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/15
 * @since 1.0
 */

//register as a REST request
@RestController
//Root path is user
@RequestMapping("/user")


public class UserController {

    private final UserService userService;

    //Construct method injected bean
    public UserController(UserService userService) {
        this.userService = userService;
    }

    //User registration
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody UserRegisterRequestDTO userRegisterRequestDTO) {
        try {
            UserVO userInfo = userService.register(userRegisterRequestDTO);
            return ResponseEntity.ok(
                    Map.of(
                            "code", 200,
                            "data", userInfo,
                            "status", "success"

                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "error"));
        }

    }

    //User login
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody UserLoginRequestDTO userLoginRequestDTO) {
        try {
            Map<String, Object> result = userService.login(userLoginRequestDTO);
            if (result.get("status").equals(true)) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.status(HttpStatus.FOUND).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "error"));
        }


    }

    //Get user information
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        UserVO user = userService.getUserInfo();
        return ResponseEntity.ok(Map.of("user", user));
    }


    //Permission testing
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> auth() {
        UserVO user = userService.getUserInfo();
        return ResponseEntity.ok(Map.of("user", user));
    }


}
