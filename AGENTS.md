# AGENTS Guide

## Project Snapshot
- Stack: Spring Boot 3.4.4 + Java 17 backend, single static frontend in `src/main/resources/static/index.html`, Playwright E2E in `e2e/`.
- App base URL: `http://localhost:8082/list-set-difference` (`server.port` and `server.servlet.context-path` in `src/main/resources/application.properties`).
- Core behavior: compute `union(section1 uploads) - union(section2 uploads)` from uploaded TXT files (`README.md`).

## Architecture and Data Flow
- HTTP layer is thin: `src/main/java/com/antalpeti/listsetdifference/controller/DifferenceController.java` validates section/file basics and delegates all state logic.
- Stateful domain logic lives in singleton `DifferenceService` (`src/main/java/com/antalpeti/listsetdifference/service/DifferenceService.java`):
  - Keeps in-memory `LinkedHashMap<String, UploadedFile>` keyed by `uploadId`.
  - Public methods are `synchronized` (thread safety around mutable in-memory registry).
  - Recomputes unions dynamically on each `computeDifference()` call; no persistence/database.
- Word parsing is line-based UTF-8, trims whitespace, drops blank lines, removes UTF-8 BOM (`\uFEFF`) from first char when present.
- Ordering semantics are intentional: `LinkedHashSet` preserves first-seen order across uploads; result ordering follows section-1 encounter order after subtracting section-2 words.
- Frontend state (`index.html`) stores `data-upload-id` per row and uses a replace flow: on re-upload in same row, it sends `DELETE /api/upload/{section}/{oldUploadId}` before new `POST /api/upload/{section}`.

## API Contract (used by UI + tests)
- `POST /api/upload/{section}` -> `UploadResponse` (`uploadId`, `wordsAdded`, `totalWordsInSection`).
- `DELETE /api/upload/{section}/{uploadId}` -> `204` on success, `404` for unknown/mismatched upload.
- `GET /api/result` -> `DifferenceResult` (`words`, has-files flags, per-section unique counts).
- `GET /api/result/download` -> `text/plain` with `Content-Disposition: attachment; filename="result-YYYYMMDD-HHmmss-SSS.txt"`.
- `POST /api/reset` clears all in-memory uploads.

## Developer Workflows
- Run app: `mvn spring-boot:run`.
- Run unit/integration tests: `mvn test`.
- Fast compile without tests: `mvn -q -DskipTests compile`.
- E2E prerequisites: `npm install` and `npx playwright install chromium`.
- E2E run: `npm run test:e2e` (Playwright can auto-start app via `webServer.command` in `playwright.config.js`).

## Project-Specific Testing Conventions
- `DifferenceServiceTest` is pure unit style (Mockito + `MockMultipartFile`) and heavily checks edge cases (BOM, dedupe, invalid section, revoke behavior).
- `DifferenceControllerIntegrationTest` uses `@SpringBootTest` + `@AutoConfigureMockMvc`; paths are `/api/...` (without context path) because MockMvc does not apply server context path here.
- Integration tests reset shared singleton state via direct `differenceService.reset()` in `@BeforeEach` for speed.
- E2E `e2e/replace-flow.spec.js` asserts request ordering during replace (`DELETE` then `POST`) and button enable/disable transitions in section 3.

## Agent Guardrails for Changes
- Preserve revoke-and-replace semantics in `index.html` and backend `removeUpload`; many tests assume this exact lifecycle.
- If modifying result/download behavior, update both REST integration assertions and Playwright expectations.
- Keep API relative paths in frontend (`const API = 'api'`) so context-path deployments keep working.
- Do not introduce persistence assumptions; current design is intentionally in-memory and reset-driven.
