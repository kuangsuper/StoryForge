package com.toonflow.entity.json;

import lombok.Data;
import java.util.List;

@Data
public class CharacterState {
    private String location;
    private String physicalState;
    private String emotionalState;
    private String powerLevel;
    private List<String> inventory;
    private List<String> knownInfo;
    private List<String> unknownInfo;
    private int lastUpdatedChapter;
}
