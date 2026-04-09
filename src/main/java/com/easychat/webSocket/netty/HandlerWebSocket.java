package com.easychat.webSocket.netty;

import com.easychat.service.IRedisService;
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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.InetAddress;


@Slf4j
@Component
@ChannelHandler.Sharable
public class HandlerWebSocket extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    @Resource
    private JWTServiceImpl jwtService;
    @Autowired
    private IRedisService redisService;
    @Autowired
    private ChannelContextUtils channelContextUtils;
    @Autowired
    private Environment environment;
//
//    private String serverIp;
//    private String serverPort;
//
//    @PostConstruct
//    public void init() {
//        try {
//            serverIp = InetAddress.getLocalHost().getHostAddress();
//            // 优先读取 System Property (支持 -Dserver.port=xxxx)
//            String sysPort = System.getProperty("server.port");
//            if (sysPort != null && !sysPort.isEmpty()) {
//                serverPort = sysPort;
//            } else {
//                serverPort = environment.getProperty("server.port", "5050");
//            }
//        } catch (Exception e) {
//            serverIp = "127.0.0.1";
//            serverPort = "5050";
//            log.error("获取服务器IP失败", e);
//        }
//    }

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
        // 尝试从 Channel 属性中获取 userId
        Attribute<Integer> attribute = channel.attr(AttributeKey.valueOf(channel.id().toString()));
        Integer userId = attribute.get();
        
        if (userId != null) {
            log.info("用户 {} 下线，清理Redis位置信息", userId);
            redisService.removeUserLocation(userId);
        }
        
        channelContextUtils.removeContext(channel);
    }

    //通道就绪后，通道有连接就会触发，一般用于初始化
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame textWebSocketFrame) throws Exception {
        Channel channel = ctx.channel();
        Attribute<Integer> attribute = channel.attr(AttributeKey.valueOf(channel.id().toString()));
        Integer userId = attribute.get();
        log.info("服务器收到来自userId（发送人）为 {} 的消息：{}" , userId , textWebSocketFrame.text());
        redisService.saveHeartBeat(userId);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //判断连接成功
        log.info("evt:{}", evt);
        /**
         * evt 触发事件
         * HandshakeComplete 表示客户端与服务器 websocket 握手连接成功
         */
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
            /**
             * 建立用户自己的 channel 通过 addContext 方法 将用户 ID和管道绑定
             */
            Integer userId = jwtService.getUserId(token);
            channelContextUtils.addContext(userId,ctx.channel());

            String serverIp = InetAddress.getLocalHost().getHostAddress();
            String serverPort = environment.getProperty("server.port");
            // 注册用户位置信息到 Redis
            String address = serverIp + ":" + serverPort;
            redisService.saveUserLocation(userId, address);
            log.info("用户 {} 上线，注册位置信息: {}", userId, address);
            
            log.info("");
        }
    }


    /***
     * 通过 url 拿到 token
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
        
        // 解析查询参数
        String[] paramPairs = queryParams[1].split("&");
        for (String pair : paramPairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        
        return null;
    }
}
