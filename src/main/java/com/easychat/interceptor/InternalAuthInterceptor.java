package com.easychat.interceptor;

import com.alibaba.fastjson.JSON;
import com.easychat.config.ClusterNodeProperties;
import com.easychat.entity.ResultVo;
import com.easychat.service.cluster.InternalRequestSigner;
import com.easychat.service.cluster.NodeRegistryService;
import com.easychat.service.cluster.UserPresenceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

@Component
public class InternalAuthInterceptor implements HandlerInterceptor {
    private final ClusterNodeProperties properties;
    private final NodeRegistryService nodeRegistryService;
    private final UserPresenceService userPresenceService;
    private final InternalRequestSigner signer;

    public InternalAuthInterceptor(ClusterNodeProperties properties, NodeRegistryService nodeRegistryService,
                                   UserPresenceService userPresenceService, InternalRequestSigner signer) {
        this.properties = properties;
        this.nodeRegistryService = nodeRegistryService;
        this.userPresenceService = userPresenceService;
        this.signer = signer;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!isAllowedSource(request.getRemoteAddr())) {
            write(response, HttpStatus.FORBIDDEN, "内部请求来源不允许");
            return false;
        }
        String nodeId = request.getHeader("X-Cluster-Node-Id");
        String timestamp = request.getHeader("X-Cluster-Timestamp");
        String nonce = request.getHeader("X-Cluster-Nonce");
        String signature = request.getHeader("X-Cluster-Signature");
        if (!signer.isConfigured() || isBlank(nodeId) || isBlank(timestamp) || isBlank(nonce) || isBlank(signature)
                || !isTimestampValid(timestamp) || nodeRegistryService.findHealthyNode(nodeId) == null) {
            write(response, HttpStatus.UNAUTHORIZED, "内部请求凭证无效");
            return false;
        }
        String expected = signer.sign(request.getMethod(), requestTarget(request), timestamp, nonce, requestBody(request));
        if (!signer.matches(expected, signature)) {
            write(response, HttpStatus.UNAUTHORIZED, "内部请求签名无效");
            return false;
        }
        if (!userPresenceService.registerRequestNonce(nodeId, nonce)) {
            write(response, HttpStatus.FORBIDDEN, "内部请求已重放");
            return false;
        }
        return true;
    }

    private byte[] requestBody(HttpServletRequest request) {
        return request instanceof CachedBodyHttpServletRequest
                ? ((CachedBodyHttpServletRequest) request).getCachedBody() : new byte[0];
    }

    private String requestTarget(HttpServletRequest request) {
        return request.getQueryString() == null ? request.getRequestURI() : request.getRequestURI() + "?" + request.getQueryString();
    }

    private boolean isTimestampValid(String timestamp) {
        try {
            long requestTime = Long.parseLong(timestamp);
            return Math.abs(System.currentTimeMillis() - requestTime) <= properties.getInternalRequestMaxAgeSeconds() * 1000L;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isAllowedSource(String remoteAddress) {
        for (String allowed : properties.getInternalAllowedIps()) {
            if (allowed.equals(remoteAddress) || (allowed.contains("/") && matchesIpv4Cidr(remoteAddress, allowed))) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesIpv4Cidr(String remoteAddress, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) { return false; }
            int prefixLength = Integer.parseInt(parts[1]);
            byte[] candidate = InetAddress.getByName(remoteAddress).getAddress();
            byte[] network = InetAddress.getByName(parts[0]).getAddress();
            if (candidate.length != 4 || network.length != 4 || prefixLength < 0 || prefixLength > 32) { return false; }
            int mask = prefixLength == 0 ? 0 : -1 << (32 - prefixLength);
            int candidateValue = ((candidate[0] & 0xff) << 24) | ((candidate[1] & 0xff) << 16)
                    | ((candidate[2] & 0xff) << 8) | (candidate[3] & 0xff);
            int networkValue = ((network[0] & 0xff) << 24) | ((network[1] & 0xff) << 16)
                    | ((network[2] & 0xff) << 8) | (network[3] & 0xff);
            return (candidateValue & mask) == (networkValue & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }

    private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JSON.toJSONString(status == HttpStatus.UNAUTHORIZED ? ResultVo.unauthorized(message) : ResultVo.failed(message)));
    }
}
