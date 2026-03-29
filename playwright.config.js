// @ts-check
const { defineConfig, devices } = require('@playwright/test');

/**
 * Playwright configuration for List-Set-Difference E2E tests.
 *
 * The webServer block starts the Spring Boot application before the test run
 * (or reuses it if it is already running on port 8082).
 *
 * @see https://playwright.dev/docs/test-configuration
 */
module.exports = defineConfig({
  testDir: './e2e',

  /** Global test timeout (ms). Allow time for async fetch + DOM updates. */
  timeout: 30_000,

  /** No automatic retries — failures should be investigated immediately. */
  retries: 0,

  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
  ],

  use: {
    /** All relative URLs in tests are resolved against this base. */
    baseURL: 'http://127.0.0.1:8082/list-set-difference',

    headless: true,
    screenshot: 'only-on-failure',
    video: 'off',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  /**
   * Start (or reuse) the Spring Boot application before running tests.
   *
   * Playwright polls `url` until it gets a non-5xx response, then runs the
   * tests.  With `reuseExistingServer: true` an already-running instance on
   * port 8082 is used without launching a second one — convenient for local
   * development.
   */
  webServer: {
    command: 'mvn spring-boot:run',
    url: 'http://127.0.0.1:8082/list-set-difference/',
    reuseExistingServer: true,
    timeout: 120_000,
    stdout: 'ignore',
    stderr: 'pipe',
  },
});

