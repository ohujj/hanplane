package com.hanplane.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    COUPON_NOT_FOUND(404, "쿠폰이 존재하지 않습니다"),
    USER_NOT_FOUND(404, "유저가 존재하지 않습니다"),
    COUPON_SOLD_OUT(400, "쿠폰이 소진되었습니다."),
    COUPON_ALREADY_ISSUED(400, "이미 발급된 쿠폰입니다."),
    LOCK_TRY_FAIL(400, "락 획득에 실패하였습니다."),
    PASSWORD_NOT_EQUAL(400, "비밀번호가 틀렸습니다."),
    COUPON_CREATE_FAIL(400, "쿠폰 생성에 실패하였습니다."),

    REFRESH_TOKEN_NOT_FOUND(404, "리프레시 토큰이 존재하지 않습니다."),
    EXPIRED_TOKEN(400, "토큰이 만료되었습니다."),
    EXPIRED_REFRESH_TOKEN(400, "리프레시 토큰이 만료되었습니다."),
    JWT_TOKEN_VALIDATE_FAIL(400, "토큰 검증에 실패하였습니다."),


    ;

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
