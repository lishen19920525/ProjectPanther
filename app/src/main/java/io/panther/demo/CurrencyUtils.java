package io.panther.demo;

import java.math.BigDecimal;

/**
 * Created by LiShen on 2018/3/26.
 * 金额计算
 */

public class CurrencyUtils {
    private static final int SCALE = 8;

    public static BigDecimal zero() {
        return formatScale(null);
    }

    public static BigDecimal formatScale(BigDecimal original) {
        if (original == null) {
            return new BigDecimal(0).setScale(SCALE, BigDecimal.ROUND_HALF_UP);
        }
        return original.setScale(SCALE, BigDecimal.ROUND_HALF_UP);
    }

    public static BigDecimal add(BigDecimal val1, BigDecimal val2) {
        return formatScale(val1.add(val2));
    }

    public static BigDecimal subtract(BigDecimal val1, BigDecimal val2) {
        return formatScale(val1.subtract(val2));
    }

    public static BigDecimal multiply(BigDecimal val1, BigDecimal val2) {
        return formatScale(val1.multiply(val2));
    }

    public static BigDecimal divide(BigDecimal val1, BigDecimal val2) {
        return formatScale(val1.divide(val2, SCALE, BigDecimal.ROUND_HALF_UP));
    }

    public static String shown(BigDecimal val) {
        if (val == null) {
            return zero().toPlainString();
        }
        return val.toPlainString();
    }
}
