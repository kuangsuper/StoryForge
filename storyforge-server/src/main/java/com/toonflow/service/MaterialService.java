package com.toonflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.toonflow.common.BizException;
import com.toonflow.common.ErrorCode;
import com.toonflow.entity.Material;
import com.toonflow.mapper.MaterialMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialMapper materialMapper;
    private final OssService ossService;

    public List<Material> list(String type, String category) {
        LambdaQueryWrapper<Material> query = new LambdaQueryWrapper<>();
        if (type != null && !type.isBlank()) query.eq(Material::getType, type);
        if (category != null && !category.isBlank()) query.eq(Material::getCategory, category);
        return materialMapper.selectList(query);
    }

    /**
     * 上传素材文件到 OSS
     *
     * @param fileBytes        文件字节数组
     * @param originalFilename 原始文件名（用于提取扩展名）
     * @param name             素材名称
     * @param type             素材类型（bgm/sfx/intro/outro/watermark）
     * @param category         分类
     * @param userId           上传用户 ID
     */
    public Material upload(byte[] fileBytes, String originalFilename,
                           String name, String type, String category, Long userId) {
        String ext = extractExt(originalFilename);
        String key = "materials/" + type + "/" + UUID.randomUUID() + ext;
        String contentType = resolveContentType(ext);

        String filePath = ossService.upload(key, fileBytes, contentType);

        Material material = new Material();
        material.setName(name);
        material.setType(type);
        material.setCategory(category);
        material.setUserId(userId);
        material.setFilePath(filePath);
        materialMapper.insert(material);
        return material;
    }

    public void delete(Long id) {
        Material material = materialMapper.selectById(id);
        if (material == null) throw new BizException(ErrorCode.NOT_FOUND);
        // 删除 OSS 文件
        if (material.getFilePath() != null) {
            String key = ossService.extractKey(material.getFilePath());
            ossService.delete(key);
        }
        materialMapper.deleteById(id);
    }

    private String extractExt(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return "." + filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String resolveContentType(String ext) {
        return switch (ext) {
            case ".mp3" -> "audio/mpeg";
            case ".wav" -> "audio/wav";
            case ".aac" -> "audio/aac";
            case ".mp4" -> "video/mp4";
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }
}
