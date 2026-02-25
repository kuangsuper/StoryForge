package com.toonflow.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsRequest {
    private String text;
    private String voiceId;
    private Double speed;
    private Double pitch;
    private String emotion;
    private String outputFormat; // "wav" | "mp3"
}
