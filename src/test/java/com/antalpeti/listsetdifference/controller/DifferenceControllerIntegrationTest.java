package com.antalpeti.listsetdifference.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for {@link DifferenceController}.
 *
 * <p>The Spring application context is started once for the entire test class.
 * State isolation between tests is achieved by calling
 * {@link com.antalpeti.listsetdifference.service.DifferenceService#reset()} directly
 * in {@code @BeforeEach}, avoiding any HTTP overhead in the reset path.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("DifferenceController – REST API integration tests")
class DifferenceControllerIntegrationTest extends DifferenceControllerIntegrationTestHelper {

    @BeforeEach
    void resetServiceState() {
        differenceService.reset();
    }

    // ─── POST /api/upload/1 ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/upload/1 with valid multipart file returns 200 and correct UploadResponse body")
    void testUploadToSection1WithValidFileReturns200AndCorrectUploadResponse() throws Exception {
        mockMvc.perform(multipart(API_UPLOAD_1).file(buildSection1File()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.section").value(SECTION_NUMBER_1))
                .andExpect(jsonPath("$.fileName").value(FILE_NAME_SECTION_1))
                .andExpect(jsonPath("$.wordsAdded").value(SECTION_1_WORDS_ADDED))
                .andExpect(jsonPath("$.totalWordsInSection").value(SECTION_1_TOTAL_WORDS));
    }

    // ─── POST /api/upload/2 ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/upload/2 with valid multipart file returns 200 and correct UploadResponse body")
    void testUploadToSection2WithValidFileReturns200AndCorrectUploadResponse() throws Exception {
        mockMvc.perform(multipart(API_UPLOAD_2).file(buildSection2File()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.section").value(SECTION_NUMBER_2))
                .andExpect(jsonPath("$.fileName").value(FILE_NAME_SECTION_2))
                .andExpect(jsonPath("$.wordsAdded").value(SECTION_2_WORDS_ADDED))
                .andExpect(jsonPath("$.totalWordsInSection").value(SECTION_2_TOTAL_WORDS));
    }

    // ─── GET /api/result ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/result before any upload returns empty words list and false section flags")
    void testGetResultBeforeAnyUploadReturnsEmptyWordsAndFalseFlags() throws Exception {
        mockMvc.perform(get(API_RESULT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.words.length()").value(EMPTY_WORD_COUNT))
                .andExpect(jsonPath("$.section1HasFiles").value(false))
                .andExpect(jsonPath("$.section2HasFiles").value(false))
                .andExpect(jsonPath("$.section1WordCount").value(ZERO_WORD_COUNT))
                .andExpect(jsonPath("$.section2WordCount").value(ZERO_WORD_COUNT));
    }

    @Test
    @DisplayName("GET /api/result after uploading to both sections returns correct set-difference (section1 minus section2)")
    void testGetResultAfterUploadingToBothSectionsReturnsCorrectSetDifference() throws Exception {
        mockMvc.perform(multipart(API_UPLOAD_1).file(buildSection1File()));
        mockMvc.perform(multipart(API_UPLOAD_2).file(buildSection2File()));

        mockMvc.perform(get(API_RESULT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.words.length()").value(DIFFERENCE_WORD_COUNT))
                .andExpect(jsonPath("$.words[0]").value(WORD_APPLE))
                .andExpect(jsonPath("$.words[1]").value(WORD_CHERRY))
                .andExpect(jsonPath("$.section1HasFiles").value(true))
                .andExpect(jsonPath("$.section2HasFiles").value(true))
                .andExpect(jsonPath("$.section1WordCount").value(SECTION_1_TOTAL_WORDS))
                .andExpect(jsonPath("$.section2WordCount").value(SECTION_2_TOTAL_WORDS));
    }

    // ─── GET /api/result/download ─────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/result/download returns Content-Type text/plain and timestamped filename in Content-Disposition")
    void testDownloadResultReturnsTextPlainWithTimestampedContentDispositionHeader() throws Exception {
        mockMvc.perform(get(API_RESULT_DOWNLOAD))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andDo(result -> {
                    final var disposition = result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
                    assertNotNull(disposition, "Content-Disposition header must be present");
                    assertTrue(
                            disposition.matches(CONTENT_DISPOSITION_PATTERN),
                            "Content-Disposition [" + disposition + "] must match pattern: " + CONTENT_DISPOSITION_PATTERN
                    );
                });
    }

    // ─── POST /api/reset ──────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/reset after uploading to both sections returns 204 and subsequent GET /api/result returns empty state")
    void testResetAfterUploadClearsAllStateAndResultBecomesEmpty() throws Exception {
        mockMvc.perform(multipart(API_UPLOAD_1).file(buildSection1File()));
        mockMvc.perform(multipart(API_UPLOAD_2).file(buildSection2File()));

        mockMvc.perform(post(API_RESET))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(API_RESULT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.words.length()").value(EMPTY_WORD_COUNT))
                .andExpect(jsonPath("$.section1HasFiles").value(false))
                .andExpect(jsonPath("$.section2HasFiles").value(false))
                .andExpect(jsonPath("$.section1WordCount").value(ZERO_WORD_COUNT))
                .andExpect(jsonPath("$.section2WordCount").value(ZERO_WORD_COUNT));
    }

    // ─── Validation: invalid section number ───────────────────────────────────

    @Test
    @DisplayName("POST /api/upload/3 returns 400 because section must be 1 or 2")
    void testUploadToInvalidSectionNumberReturns400() throws Exception {
        mockMvc.perform(multipart(API_UPLOAD_INVALID_SECTION).file(buildSection1File()))
                .andExpect(status().isBadRequest());
    }

    // ─── Validation: empty file ───────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/upload/1 with empty file returns 400")
    void testUploadEmptyFileToSection1Returns400() throws Exception {
        mockMvc.perform(multipart(API_UPLOAD_1).file(buildEmptyFile()))
                .andExpect(status().isBadRequest());
    }
}

