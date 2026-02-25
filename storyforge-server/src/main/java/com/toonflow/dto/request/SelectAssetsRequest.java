package com.toonflow.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class SelectAssetsRequest {
    private List<String> shotPrompts;
}
