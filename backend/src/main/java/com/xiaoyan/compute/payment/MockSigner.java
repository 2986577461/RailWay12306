package com.xiaoyan.compute.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
public class MockSigner {
    private final byte[] secret;

    public MockSigner(@Value("${compute.payment.mock-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String payload(String paymentNo, BigDecimal amount) {
        return paymentNo + ":" + amount.stripTrailingZeros().toPlainString();
    }

    public String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign payment payload", e);
        }
    }

    public boolean verify(String payload, String signature) {
        return signature != null && sign(payload).equalsIgnoreCase(signature);
    }
}
