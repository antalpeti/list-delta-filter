# list-set-difference

Spring Boot webalkalmazás, amely két szakaszba feltöltött TXT fájlok szókészletének
**halmazkülönbségét** számítja ki: `union(1. szakasz) − union(2. szakasz)`.

## Elérhetőség

```
http://localhost:8082/list-set-difference
```

## UI – 3 szakasz

| # | Szakasz | Leírás |
|---|---------|--------|
| **1** | **1. szakasz – TXT fájlok feltöltése** | Egy vagy több TXT fájl feltöltése. Az összes fájl szavainak uniója alkotja az 1. halmazt. |
| **2** | **2. szakasz – TXT fájlok feltöltése** | Egy vagy több TXT fájl feltöltése. Az összes fájl szavainak uniója alkotja a 2. halmazt. |
| **3** | **3. szakasz – Eredmény** | Automatikusan megjelenik: `union(1. szakasz) − union(2. szakasz)`. Menthető fájlba vagy vágólapra. |

A **↺ Visszaállítás** gomb törli az összes feltöltött adatot.

## TXT fájl formátuma

Minden sor egy szót (elemet) tartalmaz. Üres sorok és ismétlődések figyelmen kívül maradnak.
UTF-8 kódolás ajánlott (a Windows-os BOM karakter automatikusan eltávolításra kerül).

```
alma
körte
szilva
alma
```

## Logika

```
eredmény = union(1. szakasz összes fájlja) − union(2. szakasz összes fájlja)
```

Az eredmény rendezett, egyedi szavak listája (a feltöltés sorrendjében, ismétlés nélkül).

## REST API

| Metódus | Útvonal | Leírás |
|---------|---------|--------|
| `POST` | `/list-set-difference/api/upload/{section}` | TXT fájl feltöltése (section = 1 vagy 2) |
| `GET`  | `/list-set-difference/api/result` | Aktuális különbség lekérdezése (JSON) |
| `GET`  | `/list-set-difference/api/result/download` | Eredmény letöltése időbélyeges TXT fájlként |
| `POST` | `/list-set-difference/api/reset` | Összes adat törlése |

## Futtatás

```bash
mvn spring-boot:run
```

## Tesztek futtatása

```bash
mvn test
```

## Fordítás (tesztek nélkül)

```bash
mvn -q -DskipTests compile
```

## E2E tesztek (Playwright)

A Playwright-alapú UI-teszt a csere-flow-t validálja: egy már feltöltött sorban új fájl
feltöltése automatikus cserét (régi revoke + új upload) hajt végre, és a UI állapota
konzisztens marad.

### Előfeltételek

- Node.js 18+ (npm elérhető)
- Maven (a Spring Boot app indításához)

### Első futtatás előtt – telepítés

```bash
npm install
npx playwright install chromium
```

### E2E teszt futtatása

```bash
# Ha a Spring Boot app még nem fut, a Playwright automatikusan elindítja (mvn spring-boot:run).
# Ha már fut (pl. fejlesztés közben), az egyedi szerver-példány újrafelhasználásra kerül.
npm run test:e2e
```

### E2E teszt – headed módban (böngésző ablak látható)

```bash
npm run test:e2e:headed
```

### HTML teszt-riport megtekintése

```bash
npm run test:e2e:report
```

A riport a `playwright-report/` mappában kerül tárolásra (`.gitignore`-ban szerepel).


