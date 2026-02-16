package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.dto.user.RefreshTokenRequestDTO;
import com.lujianfeng.spanner.dto.user.UserLoginRequestDTO;
import com.lujianfeng.spanner.dto.user.UserRegisterRequestDTO;
import com.lujianfeng.spanner.dto.user.UserUpdateProfileRequestDTO;
import com.lujianfeng.spanner.dto.user.WalletAmountChangeRequestDTO;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.vo.user.LoginVO;
import com.lujianfeng.spanner.vo.user.PageResultVO;
import com.lujianfeng.spanner.vo.user.UserVO;
import com.lujianfeng.spanner.vo.user.WalletAccountVO;
import com.lujianfeng.spanner.vo.user.WalletChangeResultVO;
import com.lujianfeng.spanner.vo.user.WalletFlowItemVO;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        log.info(userRegisterRequestDTO.toString());
        try {
            UserVO userInfo = userService.register(userRegisterRequestDTO);
            log.info("用户注册");
            return ResponseEntity.ok(
                    Map.of(
                            "code", 200,
                            "data", userInfo,
                            "status", "success"
                    )
            );
        } catch (Exception e) {
            log.error(e.getMessage());
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
            LoginVO error = LoginVO.builder().token(null).refreshToken(null).code(500L).message("服务器内部错误").build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginVO> refresh(@RequestBody RefreshTokenRequestDTO dto) {
        try {
            LoginVO result = userService.refreshToken(dto == null ? null : dto.getRefreshToken());
            if (result.getCode() == 200L) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        } catch (Exception e) {
            LoginVO error = LoginVO.builder().token(null).refreshToken(null).code(500L).message("服务器内部错误").build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    //Get user information
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        UserVO user = userService.getUserInfo();
        return ResponseEntity.ok(Map.of("user", user));
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateMe(@RequestBody UserUpdateProfileRequestDTO dto) {
        UserVO user = userService.updateUserInfo(dto);
        return ResponseEntity.ok(Map.of("user", user));
    }

    @GetMapping("/wallet")
    public ResponseEntity<Map<String, Object>> wallet() {
        WalletAccountVO wallet = userService.getMyWallet();
        return ResponseEntity.ok(
                Map.of(
                        "code", 200,
                        "status", "success",
                        "data", wallet
                )
        );
    }

    @PostMapping("/wallet/recharge")
    public ResponseEntity<Map<String, Object>> recharge(@RequestBody WalletAmountChangeRequestDTO dto) {
        try {
            WalletChangeResultVO result = userService.rechargeMyWallet(dto);
            return ResponseEntity.ok(
                    Map.of(
                            "code", 200,
                            "status", "success",
                            "data", result
                    )
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "code", 400,
                            "status", "fail",
                            "message", ex.getMessage()
                    )
            );
        }
    }

    @PostMapping("/wallet/consume")
    public ResponseEntity<Map<String, Object>> consume(@RequestBody WalletAmountChangeRequestDTO dto) {
        try {
            WalletChangeResultVO result = userService.consumeMyWallet(dto);
            return ResponseEntity.ok(
                    Map.of(
                            "code", 200,
                            "status", "success",
                            "data", result
                    )
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "code", 400,
                            "status", "fail",
                            "message", ex.getMessage()
                    )
            );
        }
    }

    @GetMapping("/wallet/flows")
    public ResponseEntity<Map<String, Object>> walletFlows(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String changeType
    ) {
        try {
            PageResultVO<WalletFlowItemVO> result = userService.listMyWalletFlows(page, size, changeType);
            return ResponseEntity.ok(
                    Map.of(
                            "code", 200,
                            "status", "success",
                            "data", result
                    )
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "code", 400,
                            "status", "fail",
                            "message", ex.getMessage()
                    )
            );
        }
    }


    //Permission testing
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> auth() {
        UserVO user = userService.getUserInfo();
        return ResponseEntity.ok(Map.of("user", user));
    }


}
