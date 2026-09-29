# Database Performance & Execution Benchmarks

**Environment:**
* **Database:** PostgreSQL 16 + PostGIS 3.4 (Docker)
* **Dataset Size:** 100,000 businesses (SF geographical distribution)
* **Hardware:** Local Host

---

## 1. Baseline Spatial Search (Unindexed `Seq Scan`)

### Query
```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, category, avg_rating,
       ST_Distance(location, ST_SetSRID(ST_MakePoint(-122.4194, 37.7749), 4326)::geography) AS distance_meters
FROM businesses
WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(-122.4194, 37.7749), 4326)::geography, 2000)
ORDER BY distance_meters ASC
LIMIT 10;


Execution Metrics

    1. Total Execution Time: ~500.33 ms

    2. JIT Overhead: 244.18 ms (LLVM compilation triggered due to cost > 738,000)

    3. Scan Type: Parallel Sequential Scan (loops=2)

    4. Buffer I/O: shared hit=2945, read=3 (~23.5 MB memory blocks read)

    5. Rows Filtered (Brute Force): 94,388 rows rejected via CPU geodesic calculations


## 2. Optimized Spatial Search (GiST Index)

### Index Definition
```sql
CREATE INDEX idx_businesses_location_gist ON businesses USING GIST (location);

Execution Metrics

    1. Total Execution Time: 88.69 ms

    2. JIT Overhead: 0 ms (Bypassed entirely; query cost dropped from 738,348 to 75,159)

    3. Scan Type: Bitmap Index Scan + Parallel Bitmap Heap Scan (loops=2)

    4. Buffer I/O: shared hit=2990 (Index traversal phase only required 122 hits)

    5. Rows Filtered (Brute Force): 4,046 rows (down from 94,388 — a 95.7% reduction in CPU math)

Index Lookup Time: 4.34 ms


# Spatial Query Performance & Indexing Benchmarks

**Environment & Setup:**
* **Database:** PostgreSQL 16 + PostGIS 3.4 (Docker)
* **Dataset Size:** 100,000 records clustered around San Francisco coordinates (`37.7749`, `-122.4194`)
* **Spatial Column Type:** `GEOGRAPHY(Point, 4326)`
* **Workload:** Top 10 nearest businesses within a 2,000-meter radius sorted by distance

---

## Benchmark Comparison

| Metric | Baseline (Sequential Scan) | PostGIS GiST Index | Impact / Improvement |
| :--- | :--- | :--- | :--- |
| **Total Query Latency** | `500.33 ms` | `88.69 ms` | **~5.6x faster** |
| **Execution Time** | `329.96 ms` | `66.88 ms` | **~4.9x faster** |
| **Scan Strategy** | `Parallel Seq Scan` | `Bitmap Index Scan` + `Parallel Bitmap Heap Scan` | Targeted index bounding-box pruning |
| **Index Lookup Duration** | N/A | `4.34 ms` | Fast logarithmic spatial traversal |
| **Geodesic Calculations Evaluated** | 100,000 points | 9,659 candidate points | **90.3% reduction** in candidates evaluated |
| **Rows Rejected by Exact Filter** | 94,388 rows | 4,046 rows | **95.7% reduction** in CPU-heavy distance math |
| **JIT Overhead** | `244.18 ms` (LLVM compilation triggered) | `0.00 ms` (Bypassed entirely) | **100% saved** (planner cost dropped below threshold) |
| **Estimated Query Cost** | `0.00..738348.35` | `422.06..75159.07` | **~9.8x reduction** in query planner cost |
| **Buffer Access** | Shared hit: 2,945, read: 3 (~23.5 MB) | Shared hit: 2,990 (Index: 122 hits) | High cache locality; avoids table-wide heap iteration |

---

## Architectural Breakdown

### 1. Baseline Bottlenecks (Unindexed)
* **Full-Table Geodesic Math:** Without an index, the database must execute the great-circle ellipsoidal distance calculation (`ST_DWithin`) across all 100,000 rows sequentially in CPU registers.
* **JIT Compiler Stall:** Because PostGIS geodesic math is computationally expensive, PostgreSQL estimated an astronomical query cost (`738,348`), exceeding the `jit_above_cost` threshold (100,000). Compiling LLVM bytecode on the fly added an immediate **244.18 ms penalty** before the query even started scanning.
* **Heapsort Pressure:** After scanning the entire heap, 5,612 matching candidates were piped into a parallel Top-N heapsort.

### 2. GiST Spatial Optimization (Indexed)
* **Two-Phase Bounding Box Filtering:**
  1. **Phase 1 (Index Traversal):** The query uses `_st_expand` to build an axis-aligned bounding box (AABB) around the query coordinates. The GiST R-Tree index traversed the hierarchy in **4.34 ms**, identifying only 9,659 candidates via bounding-box intersection (`&&`).
  2. **Phase 2 (Exact Geodesic Verification):** Exact `ST_DWithin` spherical calculations were executed only against the candidate rows, pruning away 95.7% of redundant mathematical operations.
* **JIT Elimination:** The optimized index dropped the planner cost below the JIT threshold, eliminating the 244 ms compilation overhead completely.