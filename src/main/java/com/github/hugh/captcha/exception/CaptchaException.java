package com.github.hugh.captcha.exception;

/**
 * 工具箱运行异常类
 *
 * @author hugh
 * @version 1.0.0
 * @since JDK 1.8
 */
public class CaptchaException extends RuntimeException{
    public CaptchaException() {
    }

    public CaptchaException(String message) {
        super(message);
    }

    public CaptchaException(String message, Throwable cause) {
        super(message, cause);
    }

    public CaptchaException(Throwable cause) {
        super(cause);
    }

    public CaptchaException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
