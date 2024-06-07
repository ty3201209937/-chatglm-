package com.ttpfx.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ttpfx
 * @date 2023/3/28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
//    curl -X POST "http://127.0.0.1:8000" \
//            -H 'Content-Type: application/json' \
//            -d '{"prompt": "你好", "history": []}'
    private String role;
    private String content;
}