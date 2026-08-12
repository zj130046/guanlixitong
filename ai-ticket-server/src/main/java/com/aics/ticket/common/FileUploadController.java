package com.aics.ticket.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传/下载控制器
 * <p>
 * 上传的文件存储在 {@code upload-dir} 目录下，按日期分子目录。
 * 单文件上限 10MB，支持常见图片和文档格式。
 * </p>
 */
@RestController
@RequestMapping("/files")
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    /** 上传根目录，默认项目根目录下的 uploads/ */
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    /** 文件大小限制：10MB */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /** 允许的文件扩展名 */
    private static final java.util.Set<String> ALLOWED_EXTENSIONS = java.util.Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp",   // 图片
            "pdf", "doc", "docx", "xls", "xlsx",           // 文档
            "txt", "csv", "zip"                             // 其他
    );

    /**
     * 单文件上传
     *
     * @return { "url": "/api/files/download/2025-01-01/uuid.jpg", "filename": "original.jpg" }
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.fail(400, "请选择要上传的文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return ApiResponse.fail(400, "文件大小不能超过 10MB");
        }
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        }
        if (!ext.isEmpty() && !ALLOWED_EXTENSIONS.contains(ext)) {
            return ApiResponse.fail(400, "不支持的文件类型: ." + ext);
        }
        try {
            // 按日期分子目录
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path dir = Paths.get(uploadDir, dateDir);
            Files.createDirectories(dir);

            String storedName = UUID.randomUUID().toString();
            if (!ext.isEmpty()) storedName += "." + ext;
            Path target = dir.resolve(storedName);
            file.transferTo(target.toFile());

            String url = "/api/files/download/" + dateDir + "/" + storedName;
            log.info("文件上传成功: {} → {}", originalName, target);
            return ApiResponse.ok(Map.of("url", url, "filename", originalName != null ? originalName : storedName));
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ApiResponse.fail(500, "文件上传失败，请重试");
        }
    }

    /**
     * 多文件上传
     *
     * @return [{ "url": "...", "filename": "..." }, ...]
     */
    @PostMapping("/upload-multi")
    public ApiResponse<List<Map<String, String>>> uploadMulti(@RequestParam("files") List<MultipartFile> files) {
        List<Map<String, String>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            if (file.getSize() > MAX_FILE_SIZE) {
                return ApiResponse.fail(400, "文件 " + file.getOriginalFilename() + " 超过 10MB 限制");
            }
            try {
                ApiResponse<Map<String, String>> single = upload(file);
                if (single.getCode() == 0 && single.getData() != null) {
                    results.add(single.getData());
                }
            } catch (Exception e) {
                log.warn("多文件上传中跳过失败文件: {}", file.getOriginalFilename());
            }
        }
        return ApiResponse.ok(results);
    }

    /**
     * 文件下载/预览
     *
     * @param dateDir 日期目录，如 2025-01-01
     * @param filename 文件名，如 uuid.jpg
     */
    @GetMapping("/download/{dateDir}/{filename}")
    public ResponseEntity<Resource> download(
            @PathVariable String dateDir,
            @PathVariable String filename) {
        // 路径穿越防护
        if (dateDir.contains("..") || filename.contains("..")) {
            return ResponseEntity.badRequest().build();
        }
        Path filePath = Paths.get(uploadDir, dateDir, filename).normalize();
        File file = filePath.toFile();
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file);
        // 根据扩展名推断 Content-Type
        String contentType = "application/octet-stream";
        String name = filename.toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) contentType = "image/jpeg";
        else if (name.endsWith(".png")) contentType = "image/png";
        else if (name.endsWith(".gif")) contentType = "image/gif";
        else if (name.endsWith(".pdf")) contentType = "application/pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
