package com.toonflow.agent.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Checkpoint {
    private String currentLayer;
    private String currentStep;
    private List<String> completedList;
    private List<String> pendingList;
    private String lastAgentState;
    private LocalDateTime savedAt;
}
