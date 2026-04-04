package Nhom100.DoAnJ2EE.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility để format số tiền VND hiển thị
 */
public class CurrencyUtils {

    /**
     * Format số tiền VND thành chuỗi hiển thị
     * Ví dụ: 48000 → "48.000 ₫"
     */
    public static String formatVnd(double amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.GERMANY);
        return nf.format(amount) + " ₫";
    }

    /**
     * Format số tiền VND thành chuỗi hiển thị (không ký hiệu)
     * Ví dụ: 48000 → "48.000"
     */
    public static String formatVndNumber(double amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.GERMANY);
        return nf.format(amount);
    }
}
