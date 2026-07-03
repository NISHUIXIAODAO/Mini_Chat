package com.easychat;

import com.easychat.webSocket.netty.NettyWebSocketStarter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;

@Component("initRun")
@Slf4j
public class InitRun implements ApplicationRunner {
    private final DataSource dataSource;
    private final NettyWebSocketStarter nettyWebSocketStarter;

    public InitRun(DataSource dataSource, NettyWebSocketStarter nettyWebSocketStarter) {
        this.dataSource = dataSource;
        this.nettyWebSocketStarter = nettyWebSocketStarter;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try{
            //检验数据库连接
            dataSource.getConnection();
            //检验Netty是否成功启动
            nettyWebSocketStarter.startNetty();
        }catch (SQLException e){
            log.error("数据库连接错误！");
        }catch (Exception e){
            log.error("服务启动失败");
        }
    }
}
