package com.example.rag.common.exception;

import lombok.Getter;

/**
 * 业务异常：业务规则不满足时抛出，由全局异常处理器统一捕获。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
