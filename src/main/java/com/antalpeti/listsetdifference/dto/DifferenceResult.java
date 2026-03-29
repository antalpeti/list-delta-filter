package com.antalpeti.listsetdifference.dto;

import java.util.List;

/**
 * Result of the set-difference computation: union(section1) MINUS union(section2).
 *
 * @param words               ordered list of words in the difference set
 * @param section1HasFiles    whether at least one file has been uploaded to section 1
 * @param section2HasFiles    whether at least one file has been uploaded to section 2
 * @param section1WordCount   total unique words in section 1 union
 * @param section2WordCount   total unique words in section 2 union
 */
public record DifferenceResult(
        List<String> words,
        boolean section1HasFiles,
        boolean section2HasFiles,
        int section1WordCount,
        int section2WordCount
) {}

