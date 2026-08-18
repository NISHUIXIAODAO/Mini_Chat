package com.easychat.webSocket.netty;

import com.easychat.service.auth.WebSocketConnectionTicketService;
import com.easychat.webSocket.LocalChannelRegistry;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ChannelHandler.Sharable
public class WebSocketTicketAuthHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final WebSocketConnectionTicketService ticketService;
    private final LocalChannelRegistry localChannelRegistry;

    public WebSocketTicketAuthHandler(WebSocketConnectionTicketService ticketService,
                                      LocalChannelRegistry localChannelRegistry) {
        this.ticketService = ticketService;
        this.localChannelRegistry = localChannelRegistry;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        if (!"/ws".equals(decoder.path())) {
            ctx.fireChannelRead(ReferenceCountUtil.retain(request));
            return;
        }
        if (!HttpMethod.GET.equals(request.method())) {
            reject(ctx, "method_not_allowed", HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }

        List<String> tickets = decoder.parameters().get("ticket");
        if (tickets == null || tickets.size() != 1) {
            reject(ctx, "missing_or_ambiguous_ticket", HttpResponseStatus.UNAUTHORIZED);
            return;
        }
        Integer userId = ticketService.consume(tickets.get(0));
        if (userId == null) {
            reject(ctx, "invalid_ticket", HttpResponseStatus.UNAUTHORIZED);
            return;
        }

        localChannelRegistry.bindAuthenticatedUser(userId, ctx.channel());
        ctx.fireChannelRead(ReferenceCountUtil.retain(request));
    }

    private void reject(ChannelHandlerContext ctx, String reason, HttpResponseStatus status) {
        log.warn("WebSocket handshake rejected: {}", reason);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        ctx.writeAndFlush(response).addListener(future -> ctx.close());
    }
}
