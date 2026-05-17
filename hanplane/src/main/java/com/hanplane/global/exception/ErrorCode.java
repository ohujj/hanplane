package com.hanplane.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    //1000 - global
    ACTIVE_PROFILE_LOCAL(400, 1001, "운영 환경에서는 호출이 불가능합니다."),
    INVALID_REQUEST_PARAMETER(400, 1002, "잘못된 요청 파라미터입니다."),
    ILLEGAL_REQUEST_PARAMETER(403, 1003, "데이터에 대한 권한이 없습니다."),

    //4000 - auth
    PASSWORD_NOT_EQUAL(400, 4001,"비밀번호가 틀렸습니다."),
    REFRESH_TOKEN_NOT_FOUND(404, 4002,"리프레시 토큰이 존재하지 않습니다."),
    EXPIRED_TOKEN(400, 4003,"토큰이 만료되었습니다."),
    EXPIRED_REFRESH_TOKEN(400, 4004,"리프레시 토큰이 만료되었습니다."),
    JWT_TOKEN_VALIDATE_FAIL(400,4005, "토큰 검증에 실패하였습니다."),

    //5000 - coupon
    COUPON_NOT_FOUND(404, 5001, "쿠폰이 존재하지 않습니다"),
    COUPON_SOLD_OUT(400, 5002, "쿠폰이 소진되었습니다."),
    COUPON_ALREADY_ISSUED(400, 5003, "이미 발급된 쿠폰입니다."),
    COUPON_CREATE_FAIL(400,5004, "쿠폰 생성에 실패하였습니다."),


    //6000 - user
    USER_NOT_FOUND(404, 6001, "유저가 존재하지 않습니다"),

    //6100 - userCoupon
    USER_NOT_HAVE_COUPON_ID(400, 6101, "해당 쿠폰을 보유하고 있지 않습니다."),

    //7000 - product
    PRODUCT_NOT_FOUND(404, 7001, "상품이 존재하지 않습니다."),
    PRODUCT_SOLD_OUT(400, 7002, "상품이 품절되었습니다."),

    //8000 - order
    ORDER_NOT_FOUND(404, 8001, "주문이 존재하지 않습니다."),
    ORDER_STATUS_IS_NOT_PENDING(400, 8002, "주문의 상태가 진행 중이 아닙니다."),
    ORDER_STATUS_IS_NOT_PAID(400, 8003, "주문의 상태가 결제가 아닙니다."),
    ALREADY_REFUNDED_ORDER_ITEM(400, 8004, "이미 환불된 주문입니다."),

    //9000 - payment
    PAYMENT_NOT_FOUND(404, 9001, "결제가 존재하지 않습니다."),
    PAYMENT_IS_NOT_SUCCESS(400, 9002, "결제된 데이터가 아니라 환불이 불가능합니다."),

    //10000 - etc
    LOCK_TRY_FAIL(400, 10001,"락 획득에 실패하였습니다."),

    //11000 - portone
    PG_CALL_FAILED(400, 11001, "PG 호출에 실패하였습니다"),
    PG_PAYMENT_NOT_PAID(400, 11002, "PG 결제가 실패하였습니다."),
    PAYMENT_AMOUNT_MISMATCH(400, 11003, "PG 결제 금액 위변조 감지"),

    ;

    private final int status;
    private final int code;
    private final String message;

    ErrorCode(int status, int code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
