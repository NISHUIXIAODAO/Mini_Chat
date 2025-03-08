package com.easychat.webSocket.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
@Slf4j
@Component
@ChannelHandler.Sharable
public class NettyWebSocketStarter {
    @Value("${ws.port:}")
    private Integer wsPort;

    //建立连接 boss线程
    private static NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    //发送消息 work线程
    private static NioEventLoopGroup workGroup = new NioEventLoopGroup();

    @Autowired
    private HandlerWebSocket handlerWebSocket;
    @Autowired
    private HandlerHeartBeat handlerHeartBeat;

    @Async
    public void startNetty(){
        try{
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup,workGroup)
                    .channel(NioServerSocketChannel.class)//声明服务端使用 Java NIO 模型
                    .handler(new LoggingHandler(LogLevel.DEBUG)).childHandler(new ChannelInitializer() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            //添加业务处理器
                            ChannelPipeline pipeline = channel.pipeline();
                            //支持Http协议，使用Http的编码器，解码器
                            pipeline.addLast(new HttpServerCodec())
                                    // 聚合分片，最大聚合数据量 64KB
                                    .addLast(new HttpObjectAggregator(64 * 1024))
                                    //心跳机制  long readerIdleTime ，long writerIdleTime ，long allIdleTime ，TimeUnit unit
                                    //readerIdleTime:读超时事件，服务器一段时间内没有收到来自客户端的消息
                                    //writerIdleTime:写超时事件，客户端一段时间内没有收到来自服务端的消息
                                    //allIdleTime：  所有类型的超时时间
                                    .addLast(new IdleStateHandler(10,0,0, TimeUnit.SECONDS))
                                    //自定义 心跳超时处理器 HandlerHeartBeat
                                    .addLast(handlerHeartBeat)
                                    //将http协议升级为ws协议
                                    .addLast(new WebSocketServerProtocolHandler("/ws",null,true,64 * 1024,true,true,10000L))
                                    .addLast(handlerWebSocket);
                        }
                    });
            ChannelFuture channelFuture = serverBootstrap.bind(wsPort).sync();
            log.info("ws_port:{}",wsPort);
            log.info("Netty启动成功");
            channelFuture.channel().closeFuture().sync();
        }catch (Exception e){
            log.info("启动Netty失败" , e);
        }finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
