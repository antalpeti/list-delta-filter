# list-set-difference

Spring Boot web application that computes the **set difference** between TXT uploads
in two sections: `union(section 1) - union(section 2)`.

## URL

```
http://localhost:8082/list-set-difference
```

## UI - 3 Sections

| # | Section | Description |
|---|---------|-------------|
| **1** | **Section 1 - Upload TXT files** | Upload one or more TXT files. The union of all file words forms set 1. |
| **2** | **Section 2 - Upload TXT files** | Upload one or more TXT files. The union of all file words forms set 2. |
| **3** | **Section 3 - Result** | Automatically shows: `union(section 1) - union(section 2)`. Can be saved to file or copied to clipboard. |

The **↺ Reset** button clears all uploaded data.

## TXT File Format

Each line contains one word (element). Empty lines and duplicates are ignored.
UTF-8 encoding is recommended (Windows BOM is removed automatically).

```
apple
pear
plum
apple
```

## Logic

```
result = union(all files in section 1) - union(all files in section 2)
```

The result is an ordered, unique list of words (upload order preserved, no duplicates).

## REST API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/list-set-difference/api/upload/{section}` | Upload TXT file (`section` = 1 or 2) |
| `GET`  | `/list-set-difference/api/result` | Fetch current difference (JSON) |
| `GET`  | `/list-set-difference/api/result/download` | Download result as timestamped TXT |
| `POST` | `/list-set-difference/api/reset` | Clear all data |

## Run

```bash
mvn spring-boot:run
```

## Run Tests

```bash
mvn test
```

## Compile (Without Tests)

```bash
mvn -q -DskipTests compile
```

## E2E Tests (Playwright)

The Playwright UI test validates the replace flow: uploading a new file in an
already-uploaded row triggers automatic replacement (old revoke + new upload),
while keeping UI state consistent.

### Prerequisites

- Node.js 18+ (npm available)
- Maven (to start the Spring Boot app)

### First-Time Setup

```bash
npm install
npx playwright install chromium
```

### Run E2E Tests

```bash
# If the Spring Boot app is not running, Playwright starts it automatically (mvn spring-boot:run).
# If it is already running (for example during development), the existing server instance is reused.
npm run test:e2e
```

### Run E2E Tests in Headed Mode (visible browser window)

```bash
npm run test:e2e:headed
```

### View HTML Test Report

```bash
npm run test:e2e:report
```

The report is stored in `playwright-report/` (listed in `.gitignore`).


