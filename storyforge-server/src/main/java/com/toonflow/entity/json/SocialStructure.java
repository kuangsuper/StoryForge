package com.toonflow.entity.json;

import lombok.Data;
import java.util.List;

@Data
public class SocialStructure {
    private List<String> factions;
    private String hierarchy;
}
