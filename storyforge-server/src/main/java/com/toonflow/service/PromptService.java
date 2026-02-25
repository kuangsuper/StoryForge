package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Prompts;
import com.toonflow.mapper.PromptsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptsMapper promptsMapper;

    public List<Prompts> list() {
        return promptsMapper.selectList(null);
    }

    public void updateCustomValue(Long id, String customValue) {
        Prompts prompt = promptsMapper.selectById(id);
        if (prompt == null) throw new BizException(ErrorCode.NOT_FOUND);
        prompt.setCustomValue(customValue);
        promptsMapper.updateById(prompt);
    }

    /**
     * 根据 code 获取 Prompt 值，优先 customValue，无则 defaultValue。
     * 记录不存在时返回空字符串。
     */
    public String getPromptValue(String code) {
        Prompts prompt = promptsMapper.selectOne(
                new LambdaQueryWrapper<Prompts>().eq(Prompts::getCode, code));
        if (prompt == null) {
            log.warn("Prompt not found for code={}", code);
            return "";
        }
        if (prompt.getCustomValue() != null && !prompt.getCustomValue().isBlank()) {
            return prompt.getCustomValue();
        }
        return prompt.getDefaultValue() != null ? prompt.getDefaultValue() : "";
    }
}
