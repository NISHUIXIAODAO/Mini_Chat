package com.easychat.test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class SocketService {
    public static void main(String[] args) {
        ServerSocket server = null;
        //将连接到的客户端存储到Map中
        Map<String,Socket> CLIENT_MAP = new HashMap<>();
        try {
            server = new ServerSocket(1024);
            System.out.println("服务已启动，等待客户端连接");
            //1.实现多个客户端的连接
            while (true){
                Socket socket = server.accept();
                String ip = socket.getInetAddress().getHostAddress();
                System.out.println("有客户端连接" + ip + "端口" + socket.getPort());
                String clientKey = ip + socket.getPort();
                CLIENT_MAP.put(clientKey,socket);
                new Thread(() -> {
                    //实现不停的连接
                    while(true){
                        try {
                            InputStream inputStream = socket.getInputStream();
                            InputStreamReader inputStreamReader = new InputStreamReader(inputStream,"UTF-8");
                            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                            String readData = bufferedReader.readLine();
                            System.out.println("收到客户端消息->"+readData);

                            CLIENT_MAP.forEach((k,v)->{
                                try{
                                    OutputStream outputStream = v.getOutputStream();
                                    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream,"UTF-8"));
                                    printWriter.println(socket.getPort() + ":" + readData);
                                    printWriter.flush();
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            });
                        }catch (Exception e){
                            e.printStackTrace();
                            break;
                        }
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
