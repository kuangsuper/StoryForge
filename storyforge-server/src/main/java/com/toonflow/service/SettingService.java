package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.dto.request.UpdateSettingRequest;
import com.toonflow.entity.Setting;
import com.toonflow.mapper.SettingMapper;
import com.toonflow.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SettingService {

    private final SettingMapper settingMapper;

    public Setting get() {
        Long userId = SecurityUtil.getCurrentUserId();
        Setting setting = settingMapper.selectOne(
                new LambdaQueryWrapper<Setting>().eq(Setting::getUserId, userId));
        if (setting == null) {
            setting = new Setting();
            setting.setUserId(userId);
            settingMapper.insert(setting);
        }
        return setting;
    }

    public void update(UpdateSettingRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        Setting setting = settingMapper.selectOne(
                new LambdaQueryWrapper<Setting>().eq(Setting::getUserId, userId));
        if (setting == null) {
            setting = new Setting();
            setting.setUserId(userId);
        }
        if (request.getTokenKey() != null) setting.setTokenKey(request.getTokenKey());
        if (request.getImageModel() != null) setting.setImageModel(request.getImageModel());
        if (request.getLanguageModel() != null) setting.setLanguageModel(request.getLanguageModel());
        if (setting.getId() == null) {
            settingMapper.insert(setting);
        } else {
            settingMapper.updateById(setting);
        }
    }
}
