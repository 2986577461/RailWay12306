package com.xiaoyan.railway.common;

/**
 * Business-level exception whose message is returned to the client as {@code ApiResponse.fail(message)}.
 */
public class BizException extends RuntimeException {
    public BizException(String message) {
        super(message);
    }
}
