package com.lujianfeng.spanner.service;

import lombok.Getter;

@Getter
public class CallBizException extends RuntimeException {
    private final int code;
    private final String errorCode;

    public CallBizException(int code, String errorCode, String message) {
        super(message);
        this.code = code;
        this.errorCode = errorCode;
    }
}
