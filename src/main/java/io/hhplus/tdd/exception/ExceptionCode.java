package io.hhplus.tdd.exception;

public enum ExceptionCode {
    INVALID_AMOUNT("E001", "요청 금액은 1 이상이어야 합니다."),
    EXCEED_MAX_BALANCE("E002", "최대 잔고 2,000,000을 초과할 수 없습니다."),
    INSUFFICIENT_BALANCE("E003", "포인트가 부족합니다.");

    private final String code;
    private final String message;

    ExceptionCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}

