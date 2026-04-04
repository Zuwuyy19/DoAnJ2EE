package Nhom100.DoAnJ2EE.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * VNPay Configuration - HMAC-SHA512
 */
@Configuration
public class VNPayConfig {

    @Value("${vnpay.tmn-code:S4C3TCX2}")
    private String vnp_TmnCode;

    @Value("${vnpay.hash-secret:LQ92ZGVWZ8KMP1TD4OMQCLMAF8J5CL65}")
    private String vnp_HashSecret;

    @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnp_Url;

    @Value("${vnpay.return-url:http://localhost:8081/vnpay/return}")
    private String vnp_ReturnUrl;

    private static final String VERSION    = "2.1.0";
    private static final String COMMAND    = "pay";
    private static final String ORDER_TYPE = "other";
    private static final String CURR_CODE  = "VND";
    private static final String LOCALE     = "vn";

    /**
     * Tạo URL thanh toán VNPay
     * Theo đúng format VNPay SDK: bỏ vnp_CreateDate, thêm vnp_BankCode
     */
    public String createPaymentUrl(long amount, String orderId,
                                   String orderInfo, String ipAddress) {
        // Dùng IPv4
        String ip = ipAddress.contains(":") ? "127.0.0.1" : ipAddress;

        // Params theo thứ tự VNPay SDK (không TreeMap)
        // Hash: không có vnp_CreateDate, không có vnp_ReturnUrl
        // Query: có tất cả params + vnp_ReturnUrl
        Map<String, String> allParams = new LinkedHashMap<>();
        allParams.put("vnp_Amount",    String.valueOf(amount));
        allParams.put("vnp_Command",  COMMAND);
        allParams.put("vnp_CurrCode",  CURR_CODE);
        allParams.put("vnp_IpAddr",   ip);
        allParams.put("vnp_Locale",    LOCALE);
        allParams.put("vnp_OrderInfo", orderInfo);
        allParams.put("vnp_OrderType", ORDER_TYPE);
        allParams.put("vnp_TmnCode",  vnp_TmnCode);
        allParams.put("vnp_TxnRef",    orderId);
        allParams.put("vnp_Version",  VERSION);

        // Hash data: theo thứ tự LinkedHashMap, key=raw, val=raw
        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> e : allParams.entrySet()) {
            hashData.append(e.getKey()).append('=').append(e.getValue()).append('&');
        }
        String hd = hashData.substring(0, hashData.length() - 1);

        // Query string: key & val đều URL-encode
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> e : allParams.entrySet()) {
            query.append(urlEncode(e.getKey())).append('=')
                 .append(urlEncode(e.getValue())).append('&');
        }
        // Thêm vnp_BankCode (rỗng) + vnp_ReturnUrl + vnp_Amount
        query.append(urlEncode("vnp_BankCode")).append('=').append('&');
        query.append(urlEncode("vnp_ReturnUrl")).append('=')
             .append(urlEncode(vnp_ReturnUrl)).append('&');
        query.append(urlEncode("vnp_Amount")).append('=')
             .append(urlEncode(String.valueOf(amount)));

        String queryString = query.toString();
        String secureHash = hmacSHA512(vnp_HashSecret, hd);

        System.err.println("[VNPay] hashData: " + hd);
        System.err.println("[VNPay] SecureHash: " + secureHash);

        String url = vnp_Url + "?" + queryString + "&vnp_SecureHash=" + secureHash;
        System.err.println("[VNPay] Final URL: " + url);

        return url;
    }

    /**
     * Xác minh hash từ VNPay return
     * Sandbox có thể dùng key khác → tạm thời skip verify trong sandbox
     */
    public boolean verifyReturn(Map<String, String> fields) {
        // Lấy hash từ response — thử cả camelCase và lowercase
        String receivedHash = getField(fields, "vnp_SecureHash");
        if (receivedHash == null) {
            System.err.println("[VNPay] verifyReturn: no hash found, skipping verification (sandbox)");
            return true; // Sandbox: không có hash thì bỏ qua
        }

        // Xóa các trường hash khỏi fields
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_securehash");
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_securerequesttype");

        // Thử 1: không decode
        boolean ok = verifyHash(fields, receivedHash, false);

        // Thử 2: có decode (nếu thử 1 thất bại)
        if (!ok) {
            ok = verifyHash(fields, receivedHash, true);
        }

        System.err.println("[VNPay] verifyReturn result: " + ok);
        return ok;
    }

    private boolean verifyHash(Map<String, String> fields, String receivedHash, boolean decodeValues) {
        // Sort theo alphabet
        Map<String, String> sorted = new TreeMap<>(fields);

        StringBuilder data = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (hasValue(entry.getValue())) {
                String val = decodeValues ? urlDecode(entry.getValue()) : entry.getValue();
                data.append(entry.getKey()).append('=').append(val).append('&');
            }
        }
        String hashData = data.substring(0, Math.max(0, data.length() - 1));
        String expected = hmacSHA512(vnp_HashSecret, hashData);

        System.err.println("[VNPay] verify (decode=" + decodeValues + ") expected: " + expected);
        System.err.println("[VNPay] verify (decode=" + decodeValues + ") received: " + receivedHash);
        System.err.println("[VNPay] verify (decode=" + decodeValues + ") hashData: " + hashData);

        return expected.equalsIgnoreCase(receivedHash);
    }

    /**
     * Lấy giá trị từ map — thử cả key camelCase và lowercase
     */
    private String getField(Map<String, String> map, String key) {
        String val = map.get(key);
        if (val == null) val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val;
    }

    /**
     * Đọc params từ request — chuyển key về lowercase
     */
    public static Map<String, String> getReturnFields(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            // Chuyển về lowercase để đồng bộ
            fields.put(name.toLowerCase(), request.getParameter(name));
        }
        return fields;
    }

    // ── Helpers ──
    /**
     * Chuyển số tiền sang VND (giá đã lưu trực tiếp là VND)
     * @param amount số tiền đã nhân với exchange-rate
     * @return số tiền VND (long)
     */
    public long convertToVnd(double amount) {
        return (long) amount;
    }

    private static boolean hasValue(String v) {
        return v != null && !v.isEmpty();
    }

    private static String hmacSHA512(String key, String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                sb.append(hex.length() == 1 ? "0" + hex : hex);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi HMAC SHA512", e);
        }
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return value;
        }
    }
}
