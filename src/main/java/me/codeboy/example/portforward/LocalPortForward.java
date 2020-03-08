package me.codeboy.example.portforward;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 本地http端口转发，通过socket进行
 * Created by yuedong.li on 2020/1/30
 */
public class LocalPortForward {
    /**
     * 本机地址
     */
    private final static String HOST = "127.0.0.1";
    /**
     * 本地端口，使用该端口进行转发
     */
    private final static int NEW_PORT = 10001;
    /**
     * 请求转交到本地的端口号
     */
    private final static int FORWARD_PORT = 80;
    /**
     * 分隔符
     */
    private final static String SPLIT = "\r\n";
    /**
     * 线程池处理
     */
    private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1);

    public static void main(String[] args) throws IOException {
        new LocalPortForward().startForward();
    }

    /**
     * 开启转发
     */
    public void startForward() throws IOException {
        ServerSocket serverSocket = new ServerSocket(NEW_PORT);
        while (true) {
            Socket socket = serverSocket.accept();
            executorService.submit(() -> {
                try {
                    processSingleForward(socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void processSingleForward(Socket requestSocket) throws IOException {
        Socket forwardSocket = new Socket(HOST, FORWARD_PORT);
        BufferedReader requestBufferedReader = new BufferedReader(new InputStreamReader(requestSocket.getInputStream()));
        OutputStreamWriter forwardStreamWriter = new OutputStreamWriter(forwardSocket.getOutputStream(), StandardCharsets.UTF_8);
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
        forwardStreamWriter.write(header.toString());
        forwardStreamWriter.write(SPLIT);
        forwardStreamWriter.flush();
        forwardSocket.shutdownOutput();

        InputStream forwardInputStream = forwardSocket.getInputStream();
        OutputStream requestOutputStream = requestSocket.getOutputStream();
        byte[] bytes = new byte[8092];
        int valid = -1;
        // 将获取的内容写入原socket输出中
        while ((valid = forwardInputStream.read(bytes)) != -1) {
            requestOutputStream.write(bytes, 0, valid);
        }
        requestOutputStream.flush();
        requestOutputStream.close();
        requestBufferedReader.close();
        requestSocket.close();

        forwardStreamWriter.close();
        forwardInputStream.close();
        forwardSocket.close();
    }
}
