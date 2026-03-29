package com.antalpeti.listsetdifference.service;

import com.antalpeti.listsetdifference.dto.DifferenceResult;
import com.antalpeti.listsetdifference.dto.UploadResponse;
import com.antalpeti.listsetdifference.exception.UploadNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Stateful singleton service that tracks uploaded files per-section by a unique upload ID
 * and computes the set difference: union(section1) MINUS union(section2).
 *
 * <p>Maintaining per-file records allows individual uploads to be revoked without
 * affecting the other files in the same section.  The difference is recomputed
 * dynamically from the live registry on every call to {@link #computeDifference()}.</p>
 *
 * <p>All public methods are {@code synchronized} to ensure thread-safe access to the
 * in-memory upload registry.</p>
 */
@Service
@Slf4j
public class DifferenceService {

    // Unicode BOM that Windows Notepad inserts at the start of UTF-8 files
    private static final char BOM = '\uFEFF';

    /** Internal record holding the parsed content of a single uploaded file. */
    private record UploadedFile(String uploadId, int section, String fileName, Set<String> words) {}

    /** Insertion-ordered map from uploadId → UploadedFile; never null values. */
    private final Map<String, UploadedFile> uploads = new LinkedHashMap<>();

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Parses the uploaded TXT file line-by-line, assigns a new upload ID and stores
     * the file's unique words in the registry.
     *
     * @param section target section (1 or 2)
     * @param file    uploaded multipart file
     * @return upload summary including the generated {@code uploadId}
     * @throws IOException              if the file cannot be read
     * @throws IllegalArgumentException if {@code section} is not 1 or 2
     */
    public synchronized UploadResponse uploadFile(int section, MultipartFile file) throws IOException {
        validateSection(section);

        final var uploadId   = UUID.randomUUID().toString();
        final var fileName   = file.getOriginalFilename();
        final var parsedWords = parseWords(file);

        final var unionBefore = computeSectionUnion(section);
        uploads.put(uploadId, new UploadedFile(uploadId, section, fileName, parsedWords));
        final var unionAfter  = computeSectionUnion(section);

        final var wordsAdded = unionAfter.size() - unionBefore.size();

        log.info("Uploaded '{}' to section {} (uploadId={}) — added {} new word(s), section total: {}",
                fileName, section, uploadId, wordsAdded, unionAfter.size());

        return new UploadResponse(uploadId, section, fileName, wordsAdded, unionAfter.size());
    }

    /**
     * Removes the upload identified by {@code uploadId} from the registry.
     * The difference will be recomputed from the remaining files on the next call
     * to {@link #computeDifference()}.
     *
     * @param section  expected section of the upload (1 or 2)
     * @param uploadId the ID returned by a previous {@link #uploadFile} call
     * @throws IllegalArgumentException if {@code section} is not 1 or 2
     * @throws UploadNotFoundException  if no upload with that ID exists in the given section
     */
    public synchronized void removeUpload(int section, String uploadId) {
        validateSection(section);

        final var upload = uploads.get(uploadId);
        if (upload == null || upload.section() != section) {
            throw new UploadNotFoundException(uploadId);
        }

        uploads.remove(uploadId);
        log.info("Revoked upload '{}' (section {}, file '{}')", uploadId, section, upload.fileName());
    }

    /**
     * Computes and returns the current difference result.
     * The result is a snapshot; subsequent uploads or revocations may change it.
     *
     * <p>Subtraction is <em>case-insensitive</em>: a section-1 word is removed from the
     * result whenever a section-2 word matches it regardless of case.  The <em>original</em>
     * case of surviving section-1 words is preserved in the output.</p>
     */
    public synchronized DifferenceResult computeDifference() {
        final var section1Union = computeSectionUnion(1);
        final var section2Union = computeSectionUnion(2);

        // Build a lowercase lookup set so the subtraction is case-insensitive.
        final var section2LowerCase = section2Union.stream()
                .map(word -> word.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        // Keep only section-1 words that have no case-insensitive match in section2,
        // preserving their original case and first-seen encounter order.
        final var difference = section1Union.stream()
                .filter(word -> !section2LowerCase.contains(word.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        final var words = Collections.unmodifiableList(new ArrayList<>(difference));

        final var section1HasFiles = uploads.values().stream().anyMatch(u -> u.section() == 1);
        final var section2HasFiles = uploads.values().stream().anyMatch(u -> u.section() == 2);

        log.debug("Computed difference: s1={}, s2={}, result={} word(s)",
                section1Union.size(), section2Union.size(), words.size());

        return new DifferenceResult(words, section1HasFiles, section2HasFiles,
                section1Union.size(), section2Union.size());
    }

    /**
     * Clears all uploads and resets the registry to an empty state.
     */
    public synchronized void reset() {
        uploads.clear();
        log.info("State reset — all uploads cleared");
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void validateSection(int section) {
        if (section != 1 && section != 2) {
            throw new IllegalArgumentException("Section must be 1 or 2, got: " + section);
        }
    }

    /**
     * Reads every non-blank line from the multipart file and returns the unique words
     * in encounter order.
     *
     * <p>Normalisation pipeline (applied in order):
     * <ol>
     *   <li><strong>Trim</strong> – {@link String#strip()} removes all leading and trailing
     *       Unicode whitespace.</li>
     *   <li><strong>BOM removal</strong> – if the first character of the stripped line is the
     *       UTF-8 BOM ({@code U+FEFF}), it is dropped.  Stripping first ensures that any
     *       whitespace that precedes the BOM is removed before the guard runs.</li>
     *   <li><strong>Empty-line drop</strong> – lines that are empty after the two steps above
     *       (including lines that were whitespace-only or contained only the BOM) are
     *       discarded.</li>
     * </ol>
     * </p>
     */
    private Set<String> parseWords(MultipartFile file) throws IOException {
        try (final var reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::strip)
                    .map(line -> !line.isEmpty() && line.charAt(0) == BOM ? line.substring(1) : line)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    /**
     * Computes the union of all words contributed by files uploaded to {@code section}.
     * Returns a new mutable {@link LinkedHashSet} (insertion order preserved; safe to modify).
     */
    private Set<String> computeSectionUnion(int section) {
        return uploads.values().stream()
                .filter(u -> u.section() == section)
                .flatMap(u -> u.words().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}

