package com.toonflow.entity.json;

import lombok.Data;

@Data
public class ChatMessage {
    private String role;
    private String content;
    private Long timestamp;
}
