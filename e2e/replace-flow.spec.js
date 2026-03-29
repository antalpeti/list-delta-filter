// @ts-check
'use strict';

const { test, expect } = require('@playwright/test');
const path = require('path');

// ── Fixture paths ─────────────────────────────────────────────────────────────
const FIXTURE_S1_V1 = path.join(__dirname, 'fixtures', 'section1-v1.txt'); // apple, banana, cherry
const FIXTURE_S1_V2 = path.join(__dirname, 'fixtures', 'section1-v2.txt'); // mango,  banana, plum
const FIXTURE_S2    = path.join(__dirname, 'fixtures', 'section2.txt');    // banana, date

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Uploads `filePath` via the first upload row of `section` and waits until
 * the status icon in that row shows ✅.
 *
 * @param {import('@playwright/test').Page} page
 * @param {number} section  1 or 2
 * @param {string} filePath absolute path to the fixture file
 */
async function uploadInFirstRow(page, section, filePath) {
  const row = page.locator(`#uploadRows${section} .upload-row`).first();
  await expect(row).toBeVisible({ timeout: 15_000 });
  await expect(row.locator('input[type="file"]')).toBeVisible({ timeout: 15_000 });
  await row.locator('input[type="file"]').setInputFiles(filePath);
  await row.locator('.btn-upload').click();
  await expect(row.locator('.status-icon')).toHaveText('✅', { timeout: 15_000 });
}

/**
 * Reads the current backend-computed result JSON.
 *
 * @param {import('@playwright/test').APIRequestContext} request
 */
async function fetchResult(request) {
  const response = await request.get('/list-set-difference/api/result');
  expect(response.ok()).toBeTruthy();
  return response.json();
}

// ── Tests ─────────────────────────────────────────────────────────────────────

test.describe('Replace-flow – csere-flow automatikus ellenőrzése', () => {

  /**
   * Reset server-side state before every test so each run starts clean.
   * This is an API call, not a page navigation — much faster than a full reset.
   */
  test.beforeEach(async ({ request }) => {
    await request.post('/list-set-difference/api/reset');
  });

  test(
    'replacing a section-1 file in the same row: no duplicate in status list, result refreshed, section-3 buttons stay enabled',
    async ({ page, request }) => {

      // ── Navigate ────────────────────────────────────────────────────────────
      await page.goto('/list-set-difference/');
      await expect(page.locator('#uploadRows1 .upload-row input[type="file"]').first())
        .toBeVisible({ timeout: 15_000 });

      // ── Step 1: Upload first file to section 1 ──────────────────────────────
      // section1-v1.txt = apple, banana, cherry  (3 unique words)
      await uploadInFirstRow(page, 1, FIXTURE_S1_V1);
      const section1Row = page.locator('#uploadRows1 .upload-row').first();
      const oldUploadId = await section1Row.evaluate((el) => el.dataset.uploadId);
      expect(oldUploadId).toBeTruthy();

      // After the first upload the status list for section 1 shows 1 item.
      await expect(page.locator('#uploadedList1 li')).toHaveCount(1);

      // ── Step 2: Upload file to section 2 ────────────────────────────────────
      // section2.txt = banana, date  (2 unique words)
      // Expected initial result = {apple, cherry}  (section1 minus section2)
      await uploadInFirstRow(page, 2, FIXTURE_S2);

      await expect(page.locator('#uploadedList2 li')).toHaveCount(1);

      // Result area must be non-empty after both sections have files.
      await expect(page.locator('#resultArea')).toHaveValue(/apple/, { timeout: 10_000 });

      // ── Step 3: Replace section-1 file in THE SAME ROW ──────────────────────
      // section1-v2.txt = mango, banana, plum  (3 unique words)
      // After replace: expected result = {mango, plum}
      await section1Row.locator('input[type="file"]').setInputFiles(FIXTURE_S1_V2);

      const revokeResponsePromise = page.waitForResponse((response) =>
        response.request().method() === 'DELETE'
          && response.url().includes(`/list-set-difference/api/upload/1/${oldUploadId}`)
      );
      const replaceUploadResponsePromise = page.waitForResponse((response) =>
        response.request().method() === 'POST'
          && response.url().includes('/list-set-difference/api/upload/1')
      );

      await section1Row.locator('.btn-upload').click();

      const revokeResponse = await revokeResponsePromise;
      expect(revokeResponse.status()).toBe(204);

      const replaceUploadResponse = await replaceUploadResponsePromise;
      expect(replaceUploadResponse.status()).toBe(200);
      const replaceUploadBody = await replaceUploadResponse.json();
      const newUploadId = replaceUploadBody.uploadId;

      expect(newUploadId).toBeTruthy();
      expect(newUploadId).not.toBe(oldUploadId);

      await expect(section1Row.locator('.status-icon')).toHaveText('✅', { timeout: 15_000 });

      const listedUploadId = await page.locator('#uploadedList1 li').first().evaluate((el) => el.dataset.uploadId);
      expect(listedUploadId).toBe(newUploadId);

      // ── Assertion A: status list must NOT duplicate ──────────────────────────
      // The old entry is revoked and replaced → still exactly 1 item in section-1.
      await expect(page.locator('#uploadedList1 li')).toHaveCount(1);

      // ── Assertion B: result reflects the NEW file ────────────────────────────
      // Wait until the textarea shows a word from v2.
      await expect(page.locator('#resultArea')).toHaveValue(/mango/, { timeout: 10_000 });

      // Verify the full expected content (mango and plum in, apple out).
      const resultText = await page.locator('#resultArea').inputValue();
      expect(resultText).toContain('mango');
      expect(resultText).toContain('plum');
      expect(resultText).not.toContain('apple');   // v1 word must be gone

      // Old uploadId must already be revoked backend-side.
      const oldIdDeleteAgain = await request.delete(`/list-set-difference/api/upload/1/${oldUploadId}`);
      expect(oldIdDeleteAgain.status()).toBe(404);

      // ── Assertion C: section-3 action buttons stay active ────────────────────
      await expect(page.locator('#saveFileBtn')).toBeEnabled();
      await expect(page.locator('#saveClipBtn')).toBeEnabled();
    }
  );

  test(
    'deleting an uploaded row revokes backend upload and disables section-3 actions when section 1 becomes empty',
    async ({ page, request }) => {
      await page.goto('/list-set-difference/');

      // Keep one spare row so deleting the uploaded row is allowed.
      await page.locator('.card').nth(0).locator('.btn-add').click();
      await expect(page.locator('#uploadRows1 .upload-row')).toHaveCount(2);

      await uploadInFirstRow(page, 1, FIXTURE_S1_V1);
      await uploadInFirstRow(page, 2, FIXTURE_S2);

      await expect(page.locator('#uploadedList1 li')).toHaveCount(1);
      await expect(page.locator('#saveFileBtn')).toBeEnabled();
      await expect(page.locator('#saveClipBtn')).toBeEnabled();

      // Delete the uploaded row from section 1.
      await page.locator('#uploadRows1 .upload-row').first().locator('.btn-delete').click();

      // UI state: list entry gone, row removed, section-3 actions disabled.
      await expect(page.locator('#uploadedList1 li')).toHaveCount(0);
      await expect(page.locator('#uploadRows1 .upload-row')).toHaveCount(1);
      await expect(page.locator('#saveFileBtn')).toBeDisabled();
      await expect(page.locator('#saveClipBtn')).toBeDisabled();

      // Backend state: section 1 upload was truly revoked.
      const result = await fetchResult(request);
      expect(result.section1HasFiles).toBeFalsy();
      expect(result.section1WordCount).toBe(0);
      expect(result.section2HasFiles).toBeTruthy();
      expect(result.words).toEqual([]);
    }
  );

  test(
    'section-3 buttons are disabled initially, enabled after both sections upload, and disabled again after revoke',
    async ({ page }) => {
      await page.goto('/list-set-difference/');

      await expect(page.locator('#saveFileBtn')).toBeDisabled();
      await expect(page.locator('#saveClipBtn')).toBeDisabled();

      await uploadInFirstRow(page, 1, FIXTURE_S1_V1);
      await expect(page.locator('#saveFileBtn')).toBeDisabled();
      await expect(page.locator('#saveClipBtn')).toBeDisabled();

      await uploadInFirstRow(page, 2, FIXTURE_S2);
      await expect(page.locator('#saveFileBtn')).toBeEnabled();
      await expect(page.locator('#saveClipBtn')).toBeEnabled();

      // Add one extra row in section 2, then delete the uploaded one to trigger revoke.
      await page.locator('.card').nth(1).locator('.btn-add').click();
      await expect(page.locator('#uploadRows2 .upload-row')).toHaveCount(2);
      await page.locator('#uploadRows2 .upload-row').first().locator('.btn-delete').click();

      await expect(page.locator('#uploadedList2 li')).toHaveCount(0);
      await expect(page.locator('#saveFileBtn')).toBeDisabled();
      await expect(page.locator('#saveClipBtn')).toBeDisabled();
    }
  );
});

