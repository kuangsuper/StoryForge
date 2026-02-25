package com.toonflow.controller;

import com.toonflow.service.NovelExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/projects/{projectId}/novel/export")
@RequiredArgsConstructor
public class ExportController {

    private final NovelExportService novelExportService;

    @GetMapping("/txt")
    public ResponseEntity<byte[]> exportTxt(@PathVariable Long projectId) {
        String content = novelExportService.exportTxt(projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=novel.txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/docx")
    public ResponseEntity<byte[]> exportDocx(@PathVariable Long projectId) {
        byte[] content = novelExportService.exportDocx(projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=novel.docx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }
}
