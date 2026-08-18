package com.easychat.interceptor;

import com.easychat.config.ClusterNodeProperties;
import com.easychat.entity.cluster.ClusterNode;
import com.easychat.service.cluster.InternalRequestSigner;
import com.easychat.service.cluster.NodeRegistryService;
import com.easychat.service.cluster.UserPresenceService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InternalAuthInterceptorTest {
    @Test
    public void validRequestIsAcceptedAndReplayIsRejected() throws Exception {
        ClusterNodeProperties properties = new ClusterNodeProperties();
        properties.setInternalSecret("cluster-test-secret");
        InternalRequestSigner signer = new InternalRequestSigner(properties);
        NodeRegistryService nodeRegistryService = mock(NodeRegistryService.class);
        UserPresenceService userPresenceService = mock(UserPresenceService.class);
        when(nodeRegistryService.findHealthyNode("node-a")).thenReturn(new ClusterNode("node-a", "127.0.0.1", 5050, 5051));
        when(userPresenceService.registerRequestNonce("node-a", "nonce-a")).thenReturn(true, false);
        InternalAuthInterceptor interceptor = new InternalAuthInterceptor(properties, nodeRegistryService, userPresenceService, signer);

        CachedBodyHttpServletRequest request = signedRequest(signer);
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));

        MockHttpServletResponse replayResponse = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(signedRequest(signer), replayResponse, new Object()));
        assertEquals(403, replayResponse.getStatus());
    }

    private CachedBodyHttpServletRequest signedRequest(InternalRequestSigner signer) throws Exception {
        String body = "{\"message\":\"hello\"}";
        String timestamp = String.valueOf(System.currentTimeMillis());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/push");
        request.setRemoteAddr("127.0.0.1");
        request.setQueryString("userId=1001");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.addHeader("X-Cluster-Node-Id", "node-a");
        request.addHeader("X-Cluster-Timestamp", timestamp);
        request.addHeader("X-Cluster-Nonce", "nonce-a");
        request.addHeader("X-Cluster-Signature", signer.sign("POST", "/internal/push?userId=1001", timestamp, "nonce-a", body.getBytes(StandardCharsets.UTF_8)));
        return new CachedBodyHttpServletRequest(request);
    }
}
