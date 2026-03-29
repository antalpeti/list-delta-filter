package com.antalpeti.listsetdifference.dto;

/**
 * Response returned after a successful file upload.
 *
 * @param section              the target section (1 or 2)
 * @param fileName             original file name
 * @param wordsAdded           number of new (non-duplicate) words added by this upload
 * @param totalWordsInSection  total unique words accumulated in the section so far
 */
public record UploadResponse(
        int section,
        String fileName,
        int wordsAdded,
        int totalWordsInSection
) {}

