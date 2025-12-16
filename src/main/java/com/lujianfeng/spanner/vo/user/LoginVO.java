package com.lujianfeng.spanner.vo.user;


import lombok.Builder;
import lombok.Data;

/**
 *
 * User login
 *
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/16
 * @since 1.0
 */

@Builder
@Data
public class LoginVO {

    private Long code;
    private String message;
    private String token;
}
