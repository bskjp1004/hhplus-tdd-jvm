package io.hhplus.tdd.point;

public enum ExceptionCode {
    INVALID_AMOUNT("E001", "0 이하 금액은 충전할 수 없습니다."),
    INSUFFICIENT_BALANCE("E002", "포인트가 부족합니다.");

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

