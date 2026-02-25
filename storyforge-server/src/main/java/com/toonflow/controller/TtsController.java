package com.toonflow.controller;

import com.toonflow.common.ApiResponse;
import com.toonflow.entity.TtsAudio;
import com.toonflow.service.TtsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/tts")
@RequiredArgsConstructor
public class TtsController {

    private final TtsService ttsService;

    @PostMapping("/generate")
    public ApiResponse<Void> generate(@PathVariable Long projectId, @RequestParam Long scriptId) {
        ttsService.generate(projectId, scriptId);
        return ApiResponse.ok();
    }

    @GetMapping("/voices")
    public ApiResponse<List<Map<String, String>>> voices(@RequestParam(required = false) String manufacturer) {
        return ApiResponse.ok(ttsService.getVoices(manufacturer));
    }

    @PostMapping("/preview")
    public ResponseEntity<byte[]> preview(@PathVariable Long projectId, @RequestBody TtsPreviewRequest request) {
        byte[] audio = ttsService.preview(request.getText(), request.getVoiceId(), request.getManufacturer());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .body(audio);
    }

    @GetMapping("/audio")
    public ApiResponse<List<TtsAudio>> listAudio(@PathVariable Long projectId, @RequestParam Long scriptId) {
        return ApiResponse.ok(ttsService.listAudio(projectId, scriptId));
    }

    @Data
    static class TtsPreviewRequest {
        private String text;
        private String voiceId;
        private String manufacturer;
    }
}
