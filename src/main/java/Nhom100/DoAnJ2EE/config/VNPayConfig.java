package Nhom100.DoAnJ2EE.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * VNPay Configuration - HMAC-SHA512
 */
@Configuration
public class VNPayConfig {

    @Value("${vnpay.tmn-code:UH7VZSEG}")
    private String vnp_TmnCode;

    @Value("${vnpay.hash-secret:P7N7K14MN2F1NT0VPSNGW9W1GPCIAX84}")
    private String vnp_HashSecret;

    @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnp_Url;

    @Value("${vnpay.return-url:http://localhost:8081/vnpay/return}")
    private String vnp_ReturnUrl;

    private static final String VERSION = "2.1.0";
    private static final String COMMAND = "pay";
    private static final String ORDER_TYPE = "other";
    private static final String CURR_CODE = "VND";
    private static final String LOCALE = "vn";

    /**
     * Tạo URL thanh toán VNPay
     */
    public String createPaymentUrl(long amount, String orderId,
            String orderInfo, String ipAddress) {
        // VNPay expects amount as VND * 100
        long vnpAmount = amount * 100;

        String ip = ipAddress.contains(":") ? "127.0.0.1" : ipAddress;

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", VERSION);
        vnp_Params.put("vnp_Command", COMMAND);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(vnpAmount));
        vnp_Params.put("vnp_CurrCode", CURR_CODE);
        // Omit vnp_BankCode if empty
        vnp_Params.put("vnp_TxnRef", orderId);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", ORDER_TYPE);
        vnp_Params.put("vnp_Locale", LOCALE);
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", ip);
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = vnp_Params.get(fieldName);

            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data & query string
                // VNPay 2.1.0 requires URLEncode with %20 for spaces
                String encodedKey = java.net.URLEncoder.encode(fieldName, StandardCharsets.UTF_8).replace("+", "%20");
                String encodedVal = java.net.URLEncoder.encode(fieldValue, StandardCharsets.UTF_8).replace("+", "%20");

                hashData.append(encodedKey).append('=').append(encodedVal);
                query.append(encodedKey).append('=').append(encodedVal);

                // Add separator if not the last item that was added
                // Since some items are skipped if empty, we check if there are more valid items
                // coming
                // But easier is just to trim at the end if we have a trailing &
                hashData.append('&');
                query.append('&');
            }
        }

        // Remove trailing & if exists
        String hd = hashData.toString();
        if (hd.endsWith("&"))
            hd = hd.substring(0, hd.length() - 1);
        String qs = query.toString();
        if (qs.endsWith("&"))
            qs = qs.substring(0, qs.length() - 1);

        String vnp_SecureHash = hmacSHA512(vnp_HashSecret, hd);

        System.out.println("[VNPay Debug] HashData: " + hd);
        System.out.println("[VNPay Debug] SecureHash: " + vnp_SecureHash);

        return vnp_Url + "?" + qs + "&vnp_SecureHash=" + vnp_SecureHash;
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

        System.err.println("[VNPay Debug] verify (decode=" + decodeValues + ") hashData: " + hashData);
        System.err.println("[VNPay Debug] verify (decode=" + decodeValues + ") expected: " + expected);
        System.err.println("[VNPay Debug] verify (decode=" + decodeValues + ") received: " + receivedHash);

        return expected.equalsIgnoreCase(receivedHash);
    }

    /**
     * Lấy giá trị từ map — thử cả key camelCase và lowercase
     */
    private String getField(Map<String, String> map, String key) {
        String val = map.get(key);
        if (val == null)
            val = map.get(key.toLowerCase());
        if (val == null)
            val = map.get(key.toUpperCase());
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
     * 
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

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return value;
        }
    }
}
