package com.toonflow.entity.json;

import lombok.Data;
import java.util.List;

@Data
public class SpeechStyle {
    private String tone;
    private List<String> habits;
    private List<String> vocabulary;
    private List<String> exampleDialogues;
}
