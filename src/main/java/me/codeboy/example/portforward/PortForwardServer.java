package me.codeboy.example.portforward;

import me.codeboy.common.base.log.CBPrint;
import me.codeboy.example.utlis.IOUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 本地http端口转发到客户端指定端口，通过socket进行
 * Created by yuedong.li on 2020/1/30
 */
public class PortForwardServer {
    /**
     * 最多和200个client建立连接
     */
    private final static int MAX_FORWARD_PORT = 10200;
    private static int forwardPort = 10000;
    /**
     * 分隔符
     */
    private final static String SPLIT = "\r\n";
    /**
     * 单词开始标示语
     */
    private final static String FORWARD_START_FLAG = "__";
    /**
     * 心跳
     */
    private final static String HEAT_BEAT = "❤️";
    /**
     * 线程池处理
     */
    private ExecutorService serverExecutorService = Executors.newFixedThreadPool(MAX_FORWARD_PORT - forwardPort);

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 开启server
     *
     * @param port 代理端口
     */
    public void start(int port) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                forwardPort = forwardPort + 1;
                // 此处不进行已经关闭端口的检查等操作
                if (forwardPort > MAX_FORWARD_PORT) {
                    CBPrint.println("超出最大链接数目");
                    continue;
                }
                serverExecutorService.submit(() -> {
                    try {
                        // 启动转发server
                        startForwardServer(clientSocket, forwardPort);
                    } catch (IOException e) {
                        try {
                            clientSocket.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
            CBPrint.log("socket server exception " + System.currentTimeMillis());
            IOUtils.closeQuietly(serverSocket);
        }
    }

    /**
     * 开启转发server
     */
    public void startForwardServer(Socket forwardSocket, int forwardPort) throws IOException {
        ServerSocket serverSocket = new ServerSocket(forwardPort);
        CBPrint.println("启动端口转发服务, 服务端接口为:" + forwardPort);
        Queue<Socket> socketRequestQueue = new ConcurrentLinkedQueue<>();
        new Thread(() -> {
            try {
                processHttpRequest(socketRequestQueue, forwardSocket);
            } catch (Exception e) {
                e.printStackTrace();
                CBPrint.log("socket close and port is" + forwardPort);
                IOUtils.closeQuietly(forwardSocket);
            }
        }).start();

        // 接收socket连接
        while (true) {
            Socket requestSocket = serverSocket.accept();
            socketRequestQueue.offer(requestSocket);
            CBPrint.println("receive client request and size:" + socketRequestQueue.size());
        }
    }

    /**
     * 处理请求
     *
     * @param requestSockets 请求的socket集合
     * @param forwardSocket  转发的客户端
     * @throws Exception 异常
     */
    public void processHttpRequest(Queue<Socket> requestSockets, Socket forwardSocket) throws Exception {
        forwardSocket.setSoTimeout(30000);
        InputStream forwardInputStream = forwardSocket.getInputStream();
        OutputStreamWriter forwardStreamWriter = new OutputStreamWriter(forwardSocket.getOutputStream(), StandardCharsets.UTF_8);
        Socket requestSocket = getNextSocket(requestSockets, forwardStreamWriter);
        processRequest(requestSocket, forwardStreamWriter);
        byte[] bytes = new byte[8092];
        int valid = -1;
        OutputStream requestOutputStream = requestSocket.getOutputStream();
        // 将获取的内容写入原socket输出中
        while ((valid = forwardInputStream.read(bytes)) != -1) {
            CBPrint.println("valid=" + valid);
            requestOutputStream.write(bytes, 0, valid);
            if (valid != bytes.length) {
                CBPrint.println("close request socket");
                requestOutputStream.flush();
                requestOutputStream.close();
                requestSocket.close();
                requestSocket = getNextSocket(requestSockets, forwardStreamWriter);
                processRequest(requestSocket, forwardStreamWriter);
                requestOutputStream = requestSocket.getOutputStream();
            }
        }
        requestSocket.close();
    }

    /**
     * 获取下一个请求的socket
     */
    private Socket getNextSocket(Queue<Socket> requestSockets, OutputStreamWriter forwardStreamWriter) throws Exception {
        Socket requestSocket;
        long time = System.currentTimeMillis();
        while ((requestSocket = requestSockets.poll()) == null) {
            Thread.sleep(200);
            if (System.currentTimeMillis() - time > 10000) {
                time = System.currentTimeMillis();
                processHeartBeat(forwardStreamWriter);
            }
        }
        return requestSocket;
    }

    /**
     * 处理心跳，10s内没有操作，发送心跳
     */
    private void processHeartBeat(OutputStreamWriter forwardStreamWriter) throws IOException {
        forwardStreamWriter.write(HEAT_BEAT);
        forwardStreamWriter.write(SPLIT);
        CBPrint.println("send heart beat " + HEAT_BEAT + " and time=" + sdf.format(new Date()));
        forwardStreamWriter.flush();
    }

    /**
     * 处理请求，将请求转发给代理客户端
     */
    private void processRequest(Socket requestSocket, OutputStreamWriter forwardStreamWriter) throws IOException {
        BufferedReader requestBufferedReader = new BufferedReader(new InputStreamReader(requestSocket.getInputStream()));
        String line = null;
        //转发请求header
        StringBuilder header = new StringBuilder();
        while ((line = requestBufferedReader.readLine()) != null) {
            if (line.isEmpty()) {
                break;
            }
            header.append(line);
            header.append(SPLIT);
        }
        header.append(SPLIT);
        forwardStreamWriter.write(FORWARD_START_FLAG + Base64.getEncoder().encodeToString(header.toString().getBytes()));
        forwardStreamWriter.write(SPLIT);
        CBPrint.println("start flush, info=" + header.toString().replace(SPLIT, " "));
        forwardStreamWriter.flush();
    }
}
