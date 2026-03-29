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
