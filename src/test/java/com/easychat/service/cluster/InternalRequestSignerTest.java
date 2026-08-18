package com.easychat.service.cluster;

import com.easychat.config.ClusterNodeProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InternalRequestSignerTest {
    @Test
    public void signatureCoversRequestTargetAndBody() {
        ClusterNodeProperties properties = new ClusterNodeProperties();
        properties.setInternalSecret("cluster-test-secret");
        InternalRequestSigner signer = new InternalRequestSigner(properties);
        String signature = signer.sign("POST", "/internal/push?userId=1001", "123", "nonce", "body".getBytes(StandardCharsets.UTF_8));
        assertTrue(signer.matches(signature, signer.sign("POST", "/internal/push?userId=1001", "123", "nonce", "body".getBytes(StandardCharsets.UTF_8))));
        assertFalse(signer.matches(signature, signer.sign("POST", "/internal/push?userId=1002", "123", "nonce", "body".getBytes(StandardCharsets.UTF_8))));
        assertFalse(signer.matches(signature, signer.sign("POST", "/internal/push?userId=1001", "123", "nonce", "changed".getBytes(StandardCharsets.UTF_8))));
    }
}
