package com.easychat.webSocket.netty;

import com.easychat.service.RedisService;
import com.easychat.service.impl.JWTServiceImpl;
import com.easychat.webSocket.ChannelContextUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Slf4j
@Component
@ChannelHandler.Sharable
public class HandlerWebSocket extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    @Resource
    private JWTServiceImpl jwtService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private ChannelContextUtils channelContextUtils;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 显式捕获并处理异常
        if (cause instanceof NullPointerException) {
            log.error(" 空指针异常：", cause);
        }
//        ctx.close();   // 关闭异常连接
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("有新的连接加入......");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("有连接断开");
        channelContextUtils.removeContext(ctx.channel());
    }

    //通道就绪后，通道有连接就会触发，一般用于初始化
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame textWebSocketFrame) throws Exception {
        Channel channel = ctx.channel();
        Attribute<Integer> attribute = channel.attr(AttributeKey.valueOf(channel.id().toString()));
        Integer userId = attribute.get();
        log.info("收到来自userId为 {} 的消息：{}" , userId , textWebSocketFrame.text());
        redisService.saveHeartBeat(userId);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //判断连接成功
        log.info("evt:{}", evt);
        if(evt instanceof WebSocketServerProtocolHandler.HandshakeComplete){
            WebSocketServerProtocolHandler.HandshakeComplete complete = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            String url = complete.requestUri();
            log.info("Url:{}",url);
            String token = getToken(url);
            log.info("token:{}", token);
            if (token == null || !jwtService.verifyToken(token)) {
                log.info("token有误");
                ctx.channel().close();
                return ;
            }
            channelContextUtils.addContext(jwtService.getUserId(token),ctx.channel());
            log.info("");
        }
    }


    /***
     * 通过url 拿到token
     * @param url
     * @return
     */
    private String getToken(String url){
        if(url.isEmpty() || url.indexOf("?") == -1){
            return null;
        }
        //分割url中传的参数
        String[] queryParams = url.split("\\?");
        if (queryParams.length != 2){
            return null;
        }
        String[] params = queryParams[1].split("=");
        if (params.length != 2){
            return null;
        }
        return params[1];
    }
}
