package com.ttpfx.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttpfx.entity.UserLog;
import com.ttpfx.model.ChatModel;
import com.ttpfx.service.UserLogService;
import com.ttpfx.service.UserService;
import com.ttpfx.vo.chat.ChatRequestParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author ttpfx
 * @date 2023/3/28
 */
@Component
@ServerEndpoint("/chatWebSocket/{username}")
@Slf4j
public class ChatWebSocketServer {

    /**
     * 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
     */
    private static int onlineCount = 0;
    /**
     * concurrent包的线程安全Map，用来存放每个客户端对应的MyWebSocket对象。
     */
    private static ConcurrentHashMap<String, ChatWebSocketServer> chatWebSocketMap = new ConcurrentHashMap<>();

    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;
    /**
     * 接收的username
     */
    private String username = "";

    private UserLog userLog;

    private static UserService userService;
    private static UserLogService userLogService;

    @Resource
    public void setUserService(UserService userService) {
        ChatWebSocketServer.userService = userService;
    }

    @Resource
    public void setUserLogService(UserLogService userLogService) {
        ChatWebSocketServer.userLogService = userLogService;
    }

    private ObjectMapper objectMapper = new ObjectMapper();
    private static ChatModel chatModel;

    @Resource
    public void setChatModel(ChatModel chatModel) {
        ChatWebSocketServer.chatModel = chatModel;
    }

    ChatRequestParameter chatRequestParameter = new ChatRequestParameter();

    /**
     * 建立连接
     * @param session 会话
     * @param username 连接用户名称
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username) {
        this.session = session;
        this.username = username;
        this.userLog = new UserLog();
        // 这里的用户id不可能为null，出现null，那么就是非法请求
        try {
            this.userLog.setUserId(userService.queryByName(username).getId());
        } catch (Exception e) {
            e.printStackTrace();
            try {
                session.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        this.userLog.setUsername(username);
        chatWebSocketMap.put(username, this);
        onlineCount++;
        log.info("{}--open",username);
    }

    @OnClose
    public void onClose() {
        chatWebSocketMap.remove(username);
        log.info("{}--close",username);
    }

    public String getAnswer(String question) {
        String response = "";
        try {
            // URL to send the POST request to
            URL url = new URL("http://192.168.0.127:8000");

            // Open connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set the request method to POST
            connection.setRequestMethod("POST");

            // Set headers
            connection.setRequestProperty("Content-Type", "application/json");

            // Enable input and output streams
            connection.setDoOutput(true);

            // JSON payload
            String jsonInputString = "{\"prompt\": \"" + question + "\", \"history\": []}";

            // Write the JSON data to the request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Get the response code
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Read the response
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder responseBuilder = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    responseBuilder.append(responseLine.trim());
                }
                response = responseBuilder.toString();
                System.out.println("Response: " + response);
                JSONObject jsonObject = new JSONObject(response);
                response = jsonObject.getString("response");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println(username + "--" + message);
        // 记录日志
        this.userLog.setDateTime(LocalDateTime.now());
        this.userLog.setPreLogId(this.userLog.getLogId() == null ? -1 : this.userLog.getLogId());
        this.userLog.setLogId(null);
        this.userLog.setQuestion(message);
        long start = System.currentTimeMillis();

        // 调用getAnswer方法获取返回值
        String answer = getAnswer(message);

        long end = System.currentTimeMillis();
        this.userLog.setConsumeTime(end - start);
        this.userLog.setAnswer(answer);

        // 将答案发送给客户端
        try {
            session.getBasicRemote().sendText(answer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        userLogService.save(userLog);
    }


    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    public static void sendInfo(String message, String toUserId) throws IOException {
        chatWebSocketMap.get(toUserId).sendMessage(message);
    }
}
