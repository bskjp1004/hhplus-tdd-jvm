package io.hhplus.tdd.point.validator;

public class UserPointValidator {
    public static boolean canAdd(long requestAmount){
        return requestAmount > 0;
    }

    public static boolean canUse(long requestAmount){
        return requestAmount > 0;
    }
}
