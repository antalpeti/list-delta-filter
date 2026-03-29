package com.antalpeti.listsetdifference.service;

import com.antalpeti.listsetdifference.dto.DifferenceResult;
import com.antalpeti.listsetdifference.dto.UploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Stateful singleton service that accumulates uploaded words per section and computes
 * the set difference: union(section1) MINUS union(section2).
 *
 * <p>All public methods are {@code synchronized} to ensure thread-safe access to the
 * in-memory word sets.</p>
 */
@Service
@Slf4j
public class DifferenceService {

    // Unicode BOM that Windows Notepad inserts at the start of UTF-8 files
    private static final char BOM = '\uFEFF';

    private final Set<String> section1Words = new LinkedHashSet<>();
    private final Set<String> section2Words = new LinkedHashSet<>();

    private int section1FileCount = 0;
    private int section2FileCount = 0;

    /**
     * Parses the uploaded TXT file line-by-line and adds each non-blank line to the
     * respective section's word set (duplicates are silently ignored).
     *
     * @param section target section (1 or 2)
     * @param file    uploaded multipart file
     * @return upload summary
     * @throws IOException              if the file cannot be read
     * @throws IllegalArgumentException if {@code section} is not 1 or 2
     */
    public synchronized UploadResponse uploadFile(int section, MultipartFile file) throws IOException {
        if (section != 1 && section != 2) {
            throw new IllegalArgumentException("Section must be 1 or 2, got: " + section);
        }

        final var targetSet = section == 1 ? section1Words : section2Words;
        final var sizeBefore = targetSet.size();

        try (final var reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            reader.lines()
                    .map(line -> !line.isEmpty() && line.charAt(0) == BOM ? line.substring(1) : line)
                    .map(String::strip)
                    .filter(line -> !line.isEmpty())
                    .forEach(targetSet::add);
        }

        final var wordsAdded = targetSet.size() - sizeBefore;

        if (section == 1) {
            section1FileCount++;
        } else {
            section2FileCount++;
        }

        log.info("Uploaded '{}' to section {} — added {} new word(s), section total: {}",
                file.getOriginalFilename(), section, wordsAdded, targetSet.size());

        return new UploadResponse(section, file.getOriginalFilename(), wordsAdded, targetSet.size());
    }

    /**
     * Computes and returns the current difference result.
     * The result is a snapshot; subsequent uploads may change it.
     */
    public synchronized DifferenceResult computeDifference() {
        final var difference = new LinkedHashSet<>(section1Words);
        difference.removeAll(section2Words);

        final var words = Collections.unmodifiableList(new ArrayList<>(difference));

        log.debug("Computed difference: s1={}, s2={}, result={} word(s)",
                section1Words.size(), section2Words.size(), words.size());

        return new DifferenceResult(
                words,
                section1FileCount > 0,
                section2FileCount > 0,
                section1Words.size(),
                section2Words.size()
        );
    }

    /**
     * Clears all accumulated words and resets file counters for both sections.
     */
    public synchronized void reset() {
        section1Words.clear();
        section2Words.clear();
        section1FileCount = 0;
        section2FileCount = 0;
        log.info("State reset — all words cleared");
    }
}


