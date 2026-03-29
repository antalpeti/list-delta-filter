package com.antalpeti.listsetdifference.controller;

import com.antalpeti.listsetdifference.dto.DifferenceResult;
import com.antalpeti.listsetdifference.dto.UploadResponse;
import com.antalpeti.listsetdifference.service.DifferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * REST controller exposing the List-Set-Difference API under {@code /api}.
 *
 * <p>All paths are relative to the configured context-path
 * ({@code /list-set-difference}), so the full URL becomes
 * {@code http://localhost:8082/list-set-difference/api/…}.</p>
 *
 * <ul>
 *   <li>{@code POST /api/upload/{section}} – upload a TXT file to section 1 or 2</li>
 *   <li>{@code GET  /api/result}           – retrieve the current difference</li>
 *   <li>{@code GET  /api/result/download}  – download the result as a timestamped TXT file</li>
 *   <li>{@code POST /api/reset}            – clear all accumulated state</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DifferenceController {

    private static final DateTimeFormatter FILENAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final DifferenceService differenceService;

    /**
     * Uploads a TXT file to the given section (1 or 2).
     */
    @PostMapping("/upload/{section}")
    public ResponseEntity<?> uploadFile(
            @PathVariable int section,
            @RequestParam("file") MultipartFile file) throws IOException {

        if (section != 1 && section != 2) {
            return ResponseEntity.badRequest().body("Section must be 1 or 2");
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Uploaded file is empty");
        }

        log.info("POST /api/upload/{} — file: '{}', size: {} bytes",
                section, file.getOriginalFilename(), file.getSize());

        final var response = differenceService.uploadFile(section, file);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the current difference result as JSON.
     */
    @GetMapping("/result")
    public ResponseEntity<DifferenceResult> getResult() {
        final var result = differenceService.computeDifference();
        return ResponseEntity.ok(result);
    }

    /**
     * Returns the current difference result as a downloadable TXT file.
     * The filename contains a timestamp to avoid collisions:
     * {@code result-YYYYMMDD-HHmmss-SSS.txt}.
     */
    @GetMapping("/result/download")
    public ResponseEntity<byte[]> downloadResult() {
        final var result = differenceService.computeDifference();
        final var content = String.join("\n", result.words());
        final var bytes = content.getBytes(StandardCharsets.UTF_8);

        final var timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
        final var filename = "result-" + timestamp + ".txt";

        log.info("GET /api/result/download — {} word(s), filename: '{}'", result.words().size(), filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }

    /**
     * Clears all uploaded data from both sections.
     */
    @PostMapping("/reset")
    public ResponseEntity<Void> reset() {
        differenceService.reset();
        return ResponseEntity.noContent().build();
    }
}

