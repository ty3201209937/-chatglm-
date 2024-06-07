package com.ttpfx.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ttpfx
 * @date 2023/3/28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestParameter {
//    curl -X POST "http://127.0.0.1:8000" \
//            -H 'Content-Type: application/json' \
//            -d '{"prompt": "你好", "history": []}'
    private String model = "gpt-3.5-turbo";

    private List<ChatMessage> messages = new ArrayList<>();

    private boolean stream = true;

     public void addMessages(ChatMessage message) {
        this.messages.add(message);
    }
}
