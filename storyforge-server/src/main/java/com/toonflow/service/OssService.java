package com.toonflow.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

@Slf4j
@Service
public class OssService {

    @Value("${toonflow.oss.endpoint:}")
    private String endpoint;

    @Value("${toonflow.oss.access-key:}")
    private String accessKey;

    @Value("${toonflow.oss.secret-key:}")
    private String secretKey;

    @Value("${toonflow.oss.bucket:toonflow}")
    private String bucket;

    @Value("${toonflow.oss.base-url:}")
    private String baseUrl;

    private S3Client s3Client;
    private boolean enabled = false;

    @PostConstruct
    public void init() {
        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            log.warn("[OSS] Access key not configured, OSS upload will be disabled");
            return;
        }
        try {
            var builder = S3Client.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .region(Region.AP_EAST_1);
            if (endpoint != null && !endpoint.isBlank()) {
                builder.endpointOverride(URI.create(endpoint));
            }
            s3Client = builder.build();
            enabled = true;
            log.info("[OSS] S3 client initialized, bucket={}", bucket);
        } catch (Exception e) {
            log.warn("[OSS] Failed to initialize S3 client: {}", e.getMessage());
        }
    }

    /**
     * 上传文件，返回访问 URL
     */
    public String upload(String key, byte[] data, String contentType) {
        if (!enabled || s3Client == null) {
            log.warn("[OSS] OSS not enabled, returning placeholder path for key={}", key);
            return "/" + key;
        }
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(data));
            String url = resolveUrl(key);
            log.debug("[OSS] Uploaded key={}, url={}", key, url);
            return url;
        } catch (Exception e) {
            log.error("[OSS] Upload failed for key={}: {}", key, e.getMessage());
            return "/" + key;
        }
    }

    /**
     * 删除文件
     */
    public void delete(String key) {
        if (!enabled || s3Client == null || key == null || key.isBlank()) return;
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket).key(key).build());
            log.debug("[OSS] Deleted key={}", key);
        } catch (Exception e) {
            log.warn("[OSS] Delete failed for key={}: {}", key, e.getMessage());
        }
    }

    /**
     * 从完整 URL 提取 OSS key
     */
    public String extractKey(String url) {
        if (url == null) return null;
        String base = resolveBaseUrl();
        if (!base.isBlank() && url.startsWith(base)) {
            return url.substring(base.length()).replaceFirst("^/", "");
        }
        // 去掉开头的 /
        return url.replaceFirst("^/", "");
    }

    private String resolveUrl(String key) {
        String base = resolveBaseUrl();
        if (!base.isBlank()) return base + "/" + key;
        if (endpoint != null && !endpoint.isBlank()) return endpoint + "/" + bucket + "/" + key;
        return "/" + key;
    }

    private String resolveBaseUrl() {
        return baseUrl != null ? baseUrl.stripTrailing() : "";
    }
}
