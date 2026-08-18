package com.easychat.service.cluster;

import com.easychat.config.ClusterNodeProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalRequestSigner {
    private final ClusterNodeProperties properties;

    public InternalRequestSigner(ClusterNodeProperties properties) {
        this.properties = properties;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(properties.getInternalSecret());
    }

    public String sign(String method, String requestTarget, String timestamp, String nonce, byte[] body) {
        if (!isConfigured()) {
            throw new IllegalStateException("Cluster internal secret is not configured");
        }
        try {
            String input = method.toUpperCase() + "\n" + requestTarget + "\n" + timestamp + "\n" + nonce + "\n" + sha256(body);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getInternalSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return toHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign cluster request", e);
        }
    }

    public boolean matches(String expected, String actual) {
        return expected != null && actual != null
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), actual.getBytes(StandardCharsets.US_ASCII));
    }

    private String sha256(byte[] body) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return toHex(digest.digest(body == null ? new byte[0] : body));
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value & 0xff));
        }
        return builder.toString();
    }
}
