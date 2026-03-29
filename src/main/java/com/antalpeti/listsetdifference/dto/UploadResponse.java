package com.antalpeti.listsetdifference.dto;

/**
 * Response returned after a successful file upload.
 *
 * @param uploadId             unique identifier for this upload (UUID); used to revoke it later
 * @param section              the target section (1 or 2)
 * @param fileName             original file name
 * @param wordsAdded           number of new (non-duplicate) words added by this upload
 * @param totalWordsInSection  total unique words accumulated in the section so far
 */
public record UploadResponse(
        String uploadId,
        int section,
        String fileName,
        int wordsAdded,
        int totalWordsInSection
) {}

