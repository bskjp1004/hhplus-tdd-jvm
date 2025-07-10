package io.hhplus.tdd.point.validator;

public class UserPointValidator {
    public static final long MAX_BALANCE = 2_000_000L;

    public static boolean isValidRequestAmount(long requestAmount){
        return requestAmount > 0 && requestAmount <= MAX_BALANCE;
    }

    public static boolean canAdd(long currentAmount, long requestAmount){
        return requestAmount > 0 && currentAmount + requestAmount <= MAX_BALANCE;
    }

    public static boolean canUse(long requestAmount){
        return requestAmount > 0 && requestAmount <= MAX_BALANCE;
    }
}
