package com.antalpeti.listsetdifference.service;

import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

class DifferenceServiceHelper {

    public static final int SECTION_1 = 1;
    public static final int SECTION_2 = 2;
    public static final int INVALID_SECTION_ZERO = 0;
    public static final int INVALID_SECTION_THREE = 3;
    public static final String FILE_NAME = "words.txt";
    public static final String BOM = "\uFEFF";
    public static final String WORD_APPLE = "apple";
    public static final String WORD_BANANA = "banana";
    public static final String WORD_CHERRY = "cherry";
    public static final String WORD_DATE = "date";

    protected DifferenceService underTest() {
        return new DifferenceService();
    }

    protected MockMultipartFile createFile(String content) {
        return new MockMultipartFile("file", FILE_NAME, "text/plain",
                content.getBytes(StandardCharsets.UTF_8));
    }

    protected MockMultipartFile createBomPrefixedFile(String content) {
        return new MockMultipartFile("file", FILE_NAME, "text/plain",
                (BOM + content).getBytes(StandardCharsets.UTF_8));
    }
}

