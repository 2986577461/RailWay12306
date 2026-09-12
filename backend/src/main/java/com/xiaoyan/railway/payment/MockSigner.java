package com.xiaoyan.railway.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Mock payment-provider signature: HMAC-SHA256 over {@code paymentNo:amount}.
 * Both the "mock gateway" (create payment) and the callback verifier share this key,
 * mirroring how WeChat/Alipay sign callbacks with a shared secret.
 */
@Component
public class MockSigner {
    private final SecretKeySpec key;

    public MockSigner(@Value("${railway.pay.mock-secret}") String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("签名计算失败", e);
        }
    }

    /** Constant-time comparison to avoid timing side channels. */
    public boolean verify(String payload, String sign) {
        if (sign == null) {
            return false;
        }
        return MessageDigest.isEqual(
                sign.getBytes(StandardCharsets.UTF_8),
                sign(payload).getBytes(StandardCharsets.UTF_8));
    }
}
