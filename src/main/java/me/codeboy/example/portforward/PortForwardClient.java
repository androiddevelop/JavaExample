package me.codeboy.example.portforward;

import me.codeboy.common.base.log.CBPrint;
import me.codeboy.example.utlis.IOUtils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 服务端端口映射到本地http端口，通过socket进行
 * Created by yuedong.li on 2020/1/30
 */
public class PortForwardClient {
    /**
     * 本机地址
     */
    private final static String LOCAL_HOST = "127.0.0.1";
    /**
     * 心跳
     */
    private final static String HEAT_BEAT = "❤️";
    /**
     * 单词开始标示语
     */
    private final static String FORWARD_START_FLAG = "__";

    /**
     * 开启客户端转发
     *
     * @param clientPort 请求转交到本地的端口号
     * @param serverHost server host
     * @param serverPort 建立socket通道的server端口
     */
    public void start(int clientPort, String serverHost, int serverPort) {
        Socket requestSocket = null;
        Socket forwardSocket = null;
        try {
            requestSocket = new Socket(serverHost, serverPort);
            BufferedReader requestBufferedReader = new BufferedReader(new InputStreamReader(requestSocket.getInputStream()));
            OutputStream requestOutputStream = requestSocket.getOutputStream();
            String line;
            while ((line = requestBufferedReader.readLine()) != null) {
                if (HEAT_BEAT.equals(line)) {
                    CBPrint.println("receive heart " + HEAT_BEAT);
                    continue;
                }
                if (line.startsWith(FORWARD_START_FLAG)) {
                    forwardSocket = new Socket(LOCAL_HOST, clientPort);
                    OutputStreamWriter forwardStreamWriter = new OutputStreamWriter(forwardSocket.getOutputStream(), StandardCharsets.UTF_8);
                    forwardStreamWriter.write(new String(Base64.getDecoder().decode(line.substring(FORWARD_START_FLAG.length()))));
                    forwardStreamWriter.flush();
                    InputStream forwardInputStream = forwardSocket.getInputStream();
                    byte[] bytes = new byte[8092];
                    int valid = -1;
                    // 将获取的内容写入原socket输出中
                    while ((valid = forwardInputStream.read(bytes)) != -1) {
                        requestOutputStream.write(bytes, 0, valid);
                        if (valid != bytes.length) {
                            break;
                        }
                    }
                    requestOutputStream.flush();
                    forwardSocket.close();
                }
            }
        } catch (Exception e) {
            CBPrint.log("socket close by client " + System.currentTimeMillis());
            IOUtils.closeQuietly(requestSocket);
            IOUtils.closeQuietly(forwardSocket);
            e.printStackTrace();
        }
    }
}
