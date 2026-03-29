package com.antalpeti.listsetdifference.controller;

import com.antalpeti.listsetdifference.service.DifferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

/**
 * Helper base class for {@link DifferenceControllerIntegrationTest}.
 *
 * <p>Provides shared constants, factory methods for test data, and the
 * autowired {@link MockMvc} / {@link DifferenceService} fields used across
 * all test scenarios.</p>
 */
public class DifferenceControllerIntegrationTestHelper {

    // ─── URL constants ────────────────────────────────────────────────────────

    /**
     * In {@code @SpringBootTest(webEnvironment = MOCK)} the servlet context path
     * ({@code server.servlet.context-path=/list-set-difference}) is a server-level
     * setting that is not applied to MockMvc routing.  MockMvc dispatches requests
     * relative to the root of the dispatcher servlet, so test request paths must
     * start directly at {@code /api/...}.
     */
    public static final String API_UPLOAD_1 = "/api/upload/1";
    public static final String API_UPLOAD_2 = "/api/upload/2";
    public static final String API_UPLOAD_INVALID_SECTION = "/api/upload/3";
    public static final String API_RESULT = "/api/result";
    public static final String API_RESULT_DOWNLOAD = "/api/result/download";
    public static final String API_RESET = "/api/reset";

    /** Template for the revoke endpoint; use {@link String#format} with section and uploadId. */
    public static final String API_REVOKE_TEMPLATE = "/api/upload/%d/%s";

    // ─── Multipart field names and content-type ───────────────────────────────

    public static final String FILE_PARAM = "file";
    public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";

    // ─── File names ──────────────────────────────────────────────────────────

    public static final String FILE_NAME_SECTION_1 = "section1.txt";
    public static final String FILE_NAME_SECTION_2 = "section2.txt";
    public static final String FILE_NAME_EMPTY = "empty.txt";

    // ─── Test words ──────────────────────────────────────────────────────────

    /**
     * Section-1 file content: apple, banana, cherry (3 unique words).
     */
    public static final String SECTION_1_CONTENT = "apple\nbanana\ncherry";

    /**
     * Section-2 file content: banana, date (2 unique words).
     * <p>
     * Expected difference = section1 MINUS section2 = {apple, cherry}.
     */
    public static final String SECTION_2_CONTENT = "banana\ndate";

    public static final String WORD_APPLE = "apple";
    public static final String WORD_CHERRY = "cherry";

    // ─── Expected upload response values ─────────────────────────────────────

    public static final int SECTION_NUMBER_1 = 1;
    public static final int SECTION_NUMBER_2 = 2;

    public static final int SECTION_1_WORDS_ADDED = 3;
    public static final int SECTION_1_TOTAL_WORDS = 3;

    public static final int SECTION_2_WORDS_ADDED = 2;
    public static final int SECTION_2_TOTAL_WORDS = 2;

    // ─── Expected result values ───────────────────────────────────────────────

    public static final int DIFFERENCE_WORD_COUNT = 2;
    public static final int EMPTY_WORD_COUNT = 0;
    public static final int ZERO_WORD_COUNT = 0;

    // ─── Content-Disposition pattern ─────────────────────────────────────────

    /**
     * Regex that the {@code Content-Disposition} header value must match.
     * Example: {@code attachment; filename="result-20260329-143022-123.txt"}
     */
    public static final String CONTENT_DISPOSITION_PATTERN =
            "attachment; filename=\"result-\\d{8}-\\d{6}-\\d{3}\\.txt\"";

    // ─── Spring-managed collaborators injected by the test context ────────────

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected DifferenceService differenceService;

    // ─── Test data factory methods ────────────────────────────────────────────

    protected MockMultipartFile buildSection1File() {
        return buildMultipartFile(FILE_NAME_SECTION_1, SECTION_1_CONTENT);
    }

    protected MockMultipartFile buildSection2File() {
        return buildMultipartFile(FILE_NAME_SECTION_2, SECTION_2_CONTENT);
    }

    protected MockMultipartFile buildEmptyFile() {
        return new MockMultipartFile(FILE_PARAM, FILE_NAME_EMPTY, CONTENT_TYPE_TEXT_PLAIN, new byte[0]);
    }

    private MockMultipartFile buildMultipartFile(String fileName, String content) {
        return new MockMultipartFile(
                FILE_PARAM,
                fileName,
                CONTENT_TYPE_TEXT_PLAIN,
                content.getBytes(StandardCharsets.UTF_8));
    }
}

