package com.toonflow.entity.json;

import lombok.Data;
import java.util.List;

@Data
public class PowerSystem {
    private String name;
    private List<String> levels;
    private String rules;
    private List<String> specialAbilities;
}
