package com.platform.common.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public R<?> handleRuntime(RuntimeException e) {
        log.error("biz error", e);
        return R.fail(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleAll(Exception e) {
        log.error("system error", e);
        return R.fail(500, "系统繁忙,请稍后再试");
    }
}
