package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.dto.user.UserLoginRequestDTO;
import com.lujianfeng.spanner.dto.user.UserRegisterRequestDTO;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.vo.user.LoginVO;
import com.lujianfeng.spanner.vo.user.UserVO;
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

    @PostMapping("/login")
    public ResponseEntity<LoginVO> login(@RequestBody UserLoginRequestDTO dto) {
        try {
            LoginVO result = userService.login(dto);

            if (result.getCode() == 200L) {
                return ResponseEntity.ok(result);
            }

            // 登录失败，返回 401 Unauthorized
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);

        } catch (Exception e) {
            // 服务器异常
            LoginVO error = LoginVO.builder().token(null).code(500L).message("服务器内部错误").build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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
