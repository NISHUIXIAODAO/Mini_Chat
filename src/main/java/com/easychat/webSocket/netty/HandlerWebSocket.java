package com.easychat.webSocket.netty;

import com.easychat.service.cluster.UserPresenceService;
import com.easychat.webSocket.LocalChannelRegistry;
import com.easychat.webSocket.WebSocketSessionInitializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@ChannelHandler.Sharable
public class HandlerWebSocket extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private final LocalChannelRegistry localChannelRegistry;
    private final UserPresenceService userPresenceService;
    private final WebSocketSessionInitializer webSocketSessionInitializer;

    public HandlerWebSocket(LocalChannelRegistry localChannelRegistry,
                            UserPresenceService userPresenceService,
                            WebSocketSessionInitializer webSocketSessionInitializer) {
        this.localChannelRegistry = localChannelRegistry;
        this.userPresenceService = userPresenceService;
        this.webSocketSessionInitializer = webSocketSessionInitializer;
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("WebSocket处理异常", cause); // 打印完整异常栈
        ctx.close(); // 发生异常时关闭连接
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("有新的连接加入......");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("有连接断开");
        Channel channel = ctx.channel();
        Integer userId = channel.attr(LocalChannelRegistry.USER_ID_KEY).get();
        if (userId != null) {
            userPresenceService.disconnect(userId, localChannelRegistry.getConnectionId(channel));
        }
        localChannelRegistry.remove(channel);
    }

    //通道就绪后，通道有连接就会触发，一般用于初始化
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame textWebSocketFrame) throws Exception {
        Channel channel = ctx.channel();
        Integer userId = channel.attr(LocalChannelRegistry.USER_ID_KEY).get();
        if (userId == null) {
            log.warn("Closing WebSocket frame received before authentication");
            ctx.close();
            return;
        }
        log.debug("WebSocket frame received from authenticated user {}", userId);
        userPresenceService.refresh(userId, localChannelRegistry.getConnectionId(channel));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof WebSocketServerProtocolHandler.HandshakeComplete){
            Integer userId = ctx.channel().attr(LocalChannelRegistry.USER_ID_KEY).get();
            if (userId == null) {
                log.warn("Closing WebSocket connection without authenticated channel context");
                ctx.channel().close();
                return;
            }
            localChannelRegistry.register(userId, ctx.channel());
            userPresenceService.connect(userId, localChannelRegistry.getConnectionId(ctx.channel()));
            webSocketSessionInitializer.initialize(userId);
            log.info("用户 {} 已在节点本地注册 WebSocket 连接", userId);
        }
        ctx.fireUserEventTriggered(evt);
    }
}
