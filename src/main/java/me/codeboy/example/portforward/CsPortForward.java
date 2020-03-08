package me.codeboy.example.portforward;

/**
 * client-server模式端口转发
 * Created by yuedong.li on 2020/3/7
 */
public class CsPortForward {
    public static void main(String[] args) throws Exception {
        // 启动服务端8888端口，负责和客户端进行socket连接
        Thread serverThread = new Thread(() -> new PortForwardServer().start(8888));
        serverThread.start();

        // wait server start
        Thread.sleep(3000);

        // 映射到本地80端口, 服务端将分配代理接口，从10001开始
        Thread clientThread = new Thread(() -> new PortForwardClient().start(80, "127.0.0.1", 8888));
        clientThread.start();

        serverThread.join();
        clientThread.join();
    }
}
