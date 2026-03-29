package com.antalpeti.listsetdifference.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import com.antalpeti.listsetdifference.exception.UploadNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("DifferenceService")
class DifferenceServiceTest extends DifferenceServiceHelper {

    @Mock
    private MultipartFile mockFile;

    // ─── uploadFile – section routing ───────────────────────────────────────

    @Test
    @DisplayName("uploadFile to section 1: returns response with correct section, filename, counts")
    void testUploadFileToSection1AddsWordsAndReturnsCorrectResponse() throws IOException {
        final var service = underTest();
        final var file = createFile(WORD_APPLE + "\n" + WORD_BANANA);

        final var response = service.uploadFile(SECTION_1, file);

        assertEquals(SECTION_1, response.section());
        assertEquals(FILE_NAME, response.fileName());
        assertEquals(2, response.wordsAdded());
        assertEquals(2, response.totalWordsInSection());
    }

    @Test
    @DisplayName("uploadFile to section 2: returns response with correct section, filename, counts")
    void testUploadFileToSection2AddsWordsAndReturnsCorrectResponse() throws IOException {
        final var service = underTest();
        final var file = createFile(WORD_CHERRY + "\n" + WORD_DATE);

        final var response = service.uploadFile(SECTION_2, file);

        assertEquals(SECTION_2, response.section());
        assertEquals(FILE_NAME, response.fileName());
        assertEquals(2, response.wordsAdded());
        assertEquals(2, response.totalWordsInSection());
    }

    // ─── uploadFile – deduplication ─────────────────────────────────────────

    @Test
    @DisplayName("uploadFile: duplicate words within a single file are counted only once")
    void testUploadFileDedupesWordsWithinSingleFile() throws IOException {
        final var service = underTest();
        final var file = createFile(WORD_APPLE + "\n" + WORD_APPLE + "\n" + WORD_BANANA);

        final var response = service.uploadFile(SECTION_1, file);

        assertEquals(2, response.wordsAdded());
        assertEquals(2, response.totalWordsInSection());
    }

    @Test
    @DisplayName("uploadFile: duplicate words across multiple uploads to the same section are ignored")
    void testUploadFileDedupesWordsAcrossMultipleUploads() throws IOException {
        final var service = underTest();
        final var firstFile = createFile(WORD_APPLE + "\n" + WORD_BANANA);
        final var secondFile = createFile(WORD_BANANA + "\n" + WORD_CHERRY);

        service.uploadFile(SECTION_1, firstFile);
        final var response = service.uploadFile(SECTION_1, secondFile);

        assertEquals(1, response.wordsAdded());
        assertEquals(3, response.totalWordsInSection());
    }

    // ─── uploadFile – line filtering ────────────────────────────────────────

    @Test
    @DisplayName("uploadFile: empty lines are ignored and not counted")
    void testUploadFileIgnoresEmptyLines() throws IOException {
        final var service = underTest();
        final var file = createFile(WORD_APPLE + "\n\n" + WORD_BANANA + "\n");

        final var response = service.uploadFile(SECTION_1, file);

        assertEquals(2, response.wordsAdded());
        assertEquals(2, response.totalWordsInSection());
    }

    @Test
    @DisplayName("uploadFile: whitespace-only lines are ignored and not counted")
    void testUploadFileIgnoresWhitespaceOnlyLines() throws IOException {
        final var service = underTest();
        final var file = createFile(WORD_APPLE + "\n   \n\t\n" + WORD_BANANA);

        final var response = service.uploadFile(SECTION_1, file);

        assertEquals(2, response.wordsAdded());
        assertEquals(2, response.totalWordsInSection());
    }

    @Test
    @DisplayName("uploadFile: leading and trailing whitespace is stripped from each word")
    void testUploadFileStripsLeadingAndTrailingWhitespaceFromWords() throws IOException {
        final var service = underTest();
        final var file = createFile("  " + WORD_APPLE + "  \n\t" + WORD_BANANA + "  ");

        service.uploadFile(SECTION_1, file);
        final var result = service.computeDifference();

        assertTrue(result.words().contains(WORD_APPLE));
        assertTrue(result.words().contains(WORD_BANANA));
        assertEquals(2, result.section1WordCount());
    }

    // ─── uploadFile – BOM handling ──────────────────────────────────────────

    @Test
    @DisplayName("uploadFile: UTF-8 BOM at the start of the first line is stripped before the word is stored")
    void testUploadFileStripsBomFromFirstLine() throws IOException {
        final var service = underTest();
        final var file = createBomPrefixedFile(WORD_APPLE + "\n" + WORD_BANANA);

        service.uploadFile(SECTION_1, file);
        final var result = service.computeDifference();

        assertTrue(result.words().contains(WORD_APPLE));
        assertFalse(result.words().contains(BOM + WORD_APPLE));
        assertEquals(2, result.section1WordCount());
    }

    @Test
    @DisplayName("uploadFile: a line containing only the BOM character produces no word")
    void testUploadFileTreatsLineThatIsOnlyBomAsEmpty() throws IOException {
        final var service = underTest();
        final var file = createBomPrefixedFile("");

        final var response = service.uploadFile(SECTION_1, file);

        assertEquals(0, response.wordsAdded());
        assertEquals(0, response.totalWordsInSection());
    }

    // ─── uploadFile – invalid section ───────────────────────────────────────

    @Test
    @DisplayName("uploadFile: section 0 throws IllegalArgumentException with descriptive message")
    void testUploadFileThrowsIllegalArgumentExceptionForSectionZero() {
        final var service = underTest();
        final var file = createFile(WORD_APPLE);

        final var ex = assertThrows(IllegalArgumentException.class,
                () -> service.uploadFile(INVALID_SECTION_ZERO, file));

        assertEquals("Section must be 1 or 2, got: " + INVALID_SECTION_ZERO, ex.getMessage());
    }

    @Test
    @DisplayName("uploadFile: section 3 throws IllegalArgumentException with descriptive message")
    void testUploadFileThrowsIllegalArgumentExceptionForSectionThree() {
        final var service = underTest();
        final var file = createFile(WORD_APPLE);

        final var ex = assertThrows(IllegalArgumentException.class,
                () -> service.uploadFile(INVALID_SECTION_THREE, file));

        assertEquals("Section must be 1 or 2, got: " + INVALID_SECTION_THREE, ex.getMessage());
    }

    // ─── uploadFile – IOException propagation ───────────────────────────────

    @Test
    @DisplayName("uploadFile: IOException from the underlying stream is propagated to the caller")
    void testUploadFileThrowsIOExceptionWhenFileCannotBeRead() throws IOException {
        final var service = underTest();
        given(mockFile.getInputStream()).willThrow(new IOException("read error"));

        assertThrows(IOException.class, () -> service.uploadFile(SECTION_1, mockFile));

        then(mockFile).should().getInputStream();
    }

    // ─── computeDifference ──────────────────────────────────────────────────

    @Test
    @DisplayName("computeDifference: empty result when no files have been uploaded")
    void testComputeDifferenceReturnsEmptyResultWhenNoFilesUploaded() {
        final var service = underTest();

        final var result = service.computeDifference();

        assertTrue(result.words().isEmpty());
        assertFalse(result.section1HasFiles());
        assertFalse(result.section2HasFiles());
        assertEquals(0, result.section1WordCount());
        assertEquals(0, result.section2WordCount());
    }

    @Test
    @DisplayName("computeDifference: all section 1 words are returned when section 2 is empty")
    void testComputeDifferenceReturnsSection1WordsWhenSection2IsEmpty() throws IOException {
        final var service = underTest();
        service.uploadFile(SECTION_1, createFile(WORD_APPLE + "\n" + WORD_BANANA));

        final var result = service.computeDifference();

        assertEquals(List.of(WORD_APPLE, WORD_BANANA), result.words());
        assertTrue(result.section1HasFiles());
        assertFalse(result.section2HasFiles());
        assertEquals(2, result.section1WordCount());
        assertEquals(0, result.section2WordCount());
    }

    @Test
    @DisplayName("computeDifference: words present in both sections are excluded from the result")
    void testComputeDifferenceExcludesWordsAlsoInSection2() throws IOException {
        final var service = underTest();
        service.uploadFile(SECTION_1, createFile(WORD_APPLE + "\n" + WORD_BANANA));
        service.uploadFile(SECTION_2, createFile(WORD_BANANA + "\n" + WORD_CHERRY));

        final var result = service.computeDifference();

        assertEquals(List.of(WORD_APPLE), result.words());
        assertEquals(2, result.section1WordCount());
        assertEquals(2, result.section2WordCount());
    }

    @Test
    @DisplayName("computeDifference: words that exist only in section 2 do not appear in the result")
    void testComputeDifferenceExcludesWordsOnlyInSection2() throws IOException {
        final var service = underTest();
        service.uploadFile(SECTION_2, createFile(WORD_CHERRY + "\n" + WORD_DATE));

        final var result = service.computeDifference();

        assertTrue(result.words().isEmpty());
        assertFalse(result.section1HasFiles());
        assertTrue(result.section2HasFiles());
    }

    @Test
    @DisplayName("computeDifference: result reflects the union of all section 1 uploads")
    void testComputeDifferenceReflectsUnionOfMultipleSection1Uploads() throws IOException {
        final var service = underTest();
        service.uploadFile(SECTION_1, createFile(WORD_APPLE));
        service.uploadFile(SECTION_1, createFile(WORD_BANANA));

        final var result = service.computeDifference();

        assertEquals(2, result.words().size());
        assertTrue(result.words().contains(WORD_APPLE));
        assertTrue(result.words().contains(WORD_BANANA));
    }

    @Test
    @DisplayName("computeDifference: result is empty when every section 1 word also appears in section 2")
    void testComputeDifferenceReturnsEmptyWhenAllSection1WordsAreInSection2() throws IOException {
        final var service = underTest();
        service.uploadFile(SECTION_1, createFile(WORD_APPLE + "\n" + WORD_BANANA));
        service.uploadFile(SECTION_2, createFile(WORD_APPLE + "\n" + WORD_BANANA + "\n" + WORD_CHERRY));

        final var result = service.computeDifference();

        assertTrue(result.words().isEmpty());
        assertTrue(result.section1HasFiles());
        assertTrue(result.section2HasFiles());
    }

    @Test
    @DisplayName("computeDifference: section1HasFiles becomes true only after an upload to section 1")
    void testComputeDifferenceSection1HasFilesIsTrueOnlyAfterUploadToSection1() throws IOException {
        final var service = underTest();
        service.uploadFile(SECTION_2, createFile(WORD_CHERRY));

        final var result = service.computeDifference();

        assertFalse(result.section1HasFiles());
        assertTrue(result.section2HasFiles());
    }

    @Test
    @DisplayName("computeDifference: section2HasFiles becomes true only after an upload to section 2")
    void testComputeDifferenceSection2HasFilesIsTrueOnlyAfterUploadToSection2() throws IOException {
        final var service = underTest();
        service.uploadFile(SECTION_1, createFile(WORD_APPLE));

        final var result = service.computeDifference();

        assertTrue(result.section1HasFiles());
        assertFalse(result.section2HasFiles());
    }

    // ─── computeDifference – case-insensitive subtraction ───────────────────

    @Test
    @DisplayName("computeDifference: section2 word in lowercase removes the same word in uppercase from section1 result")
    void testComputeDifferenceIsCaseInsensitiveLowerSection2ExcludesUpperSection1() throws IOException {
        final var service = underTest();
        service.uploadFile(SECTION_1, createFile("Apple\n" + WORD_BANANA));
        service.uploadFile(SECTION_2, createFile("apple"));

        final var result = service.computeDifference();

        assertFalse(result.words().stream().anyMatch(w -> w.equalsIgnoreCase(WORD_APPLE)));
        assertEquals(List.of(WORD_BANANA), result.words());
    }

    @Test
    @DisplayName("computeDifference: section2 word in uppercase removes the same word in lowercase from section1 result")
    void testComputeDifferenceIsCaseInsensitiveUpperSection2ExcludesLowerSection1() throws IOException {
        final var service = underTest();
        service.uploadFile(SECTION_1, createFile(WORD_APPLE + "\n" + WORD_BANANA));
        service.uploadFile(SECTION_2, createFile("APPLE"));

        final var result = service.computeDifference();

        assertFalse(result.words().contains(WORD_APPLE));
        assertEquals(List.of(WORD_BANANA), result.words());
    }

    @Test
    @DisplayName("computeDifference: original case of a surviving section1 word is preserved in the result")
    void testComputeDifferencePreservesSection1OriginalCaseInResult() throws IOException {
        final var service = underTest();
        service.uploadFile(SECTION_1, createFile("Apple\nBanana"));
        service.uploadFile(SECTION_2, createFile("banana"));

        final var result = service.computeDifference();

        assertEquals(List.of("Apple"), result.words());
        assertFalse(result.words().contains("Banana"));
        assertFalse(result.words().contains("apple"));
    }

    @Test
    @DisplayName("computeDifference: all case-variants of a section1 word are removed when section2 contains any matching case")
    void testComputeDifferenceRemovesAllCaseVariantsMatchedBySection2Word() throws IOException {
        final var service = underTest();
        service.uploadFile(SECTION_1, createFile("Apple\nAPPLE\nappLe\n" + WORD_BANANA));
        service.uploadFile(SECTION_2, createFile("apple"));

        final var result = service.computeDifference();

        assertFalse(result.words().stream().anyMatch(w -> w.equalsIgnoreCase(WORD_APPLE)));
        assertEquals(List.of(WORD_BANANA), result.words());
    }

    @Test
    @DisplayName("computeDifference: result is empty when every section1 word matches a section2 word case-insensitively")
    void testComputeDifferenceReturnsEmptyWhenAllSection1WordsMatchSection2CaseInsensitively() throws IOException {
        final var service = underTest();
        service.uploadFile(SECTION_1, createFile("Apple\nBANANA"));
        service.uploadFile(SECTION_2, createFile("APPLE\nbanana"));

        final var result = service.computeDifference();

        assertTrue(result.words().isEmpty());
    }

    // ─── reset ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("reset: clears all words from both sections so word counts return to zero")
    void testResetClearsAllWordsFromBothSections() throws IOException {
        final var service = underTest();
        service.uploadFile(SECTION_1, createFile(WORD_APPLE));
        service.uploadFile(SECTION_2, createFile(WORD_BANANA));

        service.reset();

        final var result = service.computeDifference();
        assertTrue(result.words().isEmpty());
        assertEquals(0, result.section1WordCount());
        assertEquals(0, result.section2WordCount());
    }

    @Test
    @DisplayName("reset: resets hasFiles flags for both sections so they become false again")
    void testResetResetsHasFilesFlagsForBothSections() throws IOException {
        final var service = underTest();
        service.uploadFile(SECTION_1, createFile(WORD_APPLE));
        service.uploadFile(SECTION_2, createFile(WORD_BANANA));

        service.reset();

        final var result = service.computeDifference();
        assertFalse(result.section1HasFiles());
        assertFalse(result.section2HasFiles());
    }

    @Test
    @DisplayName("reset: new uploads after reset are treated as a fresh state")
    void testResetAllowsFreshUploadsAfterClear() throws IOException {
        final var service = underTest();
        service.uploadFile(SECTION_1, createFile(WORD_APPLE + "\n" + WORD_BANANA));
        service.uploadFile(SECTION_2, createFile(WORD_BANANA));
        service.reset();

        service.uploadFile(SECTION_1, createFile(WORD_CHERRY));
        final var result = service.computeDifference();

        assertEquals(List.of(WORD_CHERRY), result.words());
        assertEquals(1, result.section1WordCount());
        assertEquals(0, result.section2WordCount());
    }

    // ─── removeUpload ────────────────────────────────────────────────────────

    @Test
    @DisplayName("removeUpload: revoking a section-1 file removes its words from the difference")
    void testRemoveUploadRevokesWordsFromDifference() throws IOException {
        final var service = underTest();
        service.uploadFile(SECTION_1, createFile(WORD_APPLE + "\n" + WORD_BANANA));
        final var response = service.uploadFile(SECTION_1, createFile(WORD_CHERRY));

        service.removeUpload(SECTION_1, response.uploadId());
        final var result = service.computeDifference();

        assertTrue(result.words().contains(WORD_APPLE));
        assertTrue(result.words().contains(WORD_BANANA));
        assertFalse(result.words().contains(WORD_CHERRY));
        assertEquals(2, result.section1WordCount());
    }

    @Test
    @DisplayName("removeUpload: revoking the only section-1 file makes section1HasFiles false")
    void testRemoveUploadLastFileInSection1MakesSectionEmpty() throws IOException {
        final var service = underTest();
        final var response = service.uploadFile(SECTION_1, createFile(WORD_APPLE));

        service.removeUpload(SECTION_1, response.uploadId());
        final var result = service.computeDifference();

        assertFalse(result.section1HasFiles());
        assertEquals(0, result.section1WordCount());
        assertTrue(result.words().isEmpty());
    }

    @Test
    @DisplayName("removeUpload: revoking a section-2 file that had masked section-1 words restores them in the result")
    void testRemoveUploadFromSection2UnmasksSection1Words() throws IOException {
        final var service = underTest();
        service.uploadFile(SECTION_1, createFile(WORD_APPLE + "\n" + WORD_BANANA));
        final var response = service.uploadFile(SECTION_2, createFile(WORD_APPLE));

        service.removeUpload(SECTION_2, response.uploadId());
        final var result = service.computeDifference();

        assertTrue(result.words().contains(WORD_APPLE));
        assertTrue(result.words().contains(WORD_BANANA));
    }

    @Test
    @DisplayName("removeUpload: response uploadId is unique per upload")
    void testUploadFileReturnsUniqueUploadIds() throws IOException {
        final var service = underTest();
        final var r1 = service.uploadFile(SECTION_1, createFile(WORD_APPLE));
        final var r2 = service.uploadFile(SECTION_1, createFile(WORD_BANANA));

        assertFalse(r1.uploadId().equals(r2.uploadId()));
    }

    @Test
    @DisplayName("removeUpload: invalid section 0 throws IllegalArgumentException")
    void testRemoveUploadThrowsForInvalidSectionZero() {
        final var service = underTest();
        assertThrows(IllegalArgumentException.class,
                () -> service.removeUpload(INVALID_SECTION_ZERO, "any-id"));
    }

    @Test
    @DisplayName("removeUpload: invalid section 3 throws IllegalArgumentException")
    void testRemoveUploadThrowsForInvalidSectionThree() {
        final var service = underTest();
        assertThrows(IllegalArgumentException.class,
                () -> service.removeUpload(INVALID_SECTION_THREE, "any-id"));
    }

    @Test
    @DisplayName("removeUpload: unknown uploadId throws UploadNotFoundException")
    void testRemoveUploadThrowsUploadNotFoundForUnknownId() {
        final var service = underTest();
        assertThrows(UploadNotFoundException.class,
                () -> service.removeUpload(SECTION_1, "non-existent-id"));
    }

    @Test
    @DisplayName("removeUpload: uploadId that belongs to the other section throws UploadNotFoundException")
    void testRemoveUploadThrowsWhenUploadIdBelongsToDifferentSection() throws IOException {
        final var service  = underTest();
        final var response = service.uploadFile(SECTION_2, createFile(WORD_APPLE));

        assertThrows(UploadNotFoundException.class,
                () -> service.removeUpload(SECTION_1, response.uploadId()));
    }
}

