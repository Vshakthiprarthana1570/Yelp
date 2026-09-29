# Spatial Query Performance & Indexing Benchmarks

**Environment & Setup:**
* **Database:** PostgreSQL 16 + PostGIS 3.4 (Docker container)
* **Dataset Size:** 100,000 synthetic records clustered around San Francisco (`37.7749`, `-122.4194`)
* **Spatial Column Type:** `GEOGRAPHY(Point, 4326)`
* **Workload:** Top 10 nearest businesses within a 2,000-meter radius sorted by distance

---

## Benchmark Comparison

| Metric | Baseline (Sequential Scan) | PostGIS GiST Index | Impact / Improvement |
| :--- | :--- | :--- | :--- |
| **Total Query Latency** | `500.33 ms` | `88.69 ms` | **~5.6x faster** |
| **Execution Time** | `329.96 ms` | `66.88 ms` | **~4.9x faster** |
| **Scan Strategy** | `Parallel Seq Scan` | `Bitmap Index Scan` + `Parallel Bitmap Heap Scan` | Targeted bounding-box pruning |
| **Index Lookup Duration** | N/A | `4.34 ms` | Fast logarithmic spatial traversal |
| **Geodesic Calculations Evaluated** | 100,000 points | 9,659 candidate points | **90.3% reduction** in candidates evaluated |
| **Rows Rejected by Exact Filter** | 94,388 rows | 4,046 rows | **95.7% reduction** in CPU-heavy distance math |
| **JIT Overhead** | `244.18 ms` (LLVM compilation) | `0.00 ms` (Bypassed entirely) | **100% saved** (planner cost dropped below threshold) |
| **Estimated Query Cost** | `0.00..738348.35` | `422.06..75159.07` | **~9.8x reduction** in query planner cost |
| **Buffer Access** | Shared hit: 2,945, read: 3 (~23.5 MB) | Shared hit: 2,990 (Index: 122 hits) | High cache locality; avoids table-wide heap iteration |

---

## 1. Baseline Spatial Search (Unindexed)

### Query
```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, name, category, avg_rating,
       ST_Distance(location, ST_SetSRID(ST_MakePoint(-122.4194, 37.7749), 4326)::geography) AS distance_meters
FROM businesses
WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(-122.4194, 37.7749), 4326)::geography, 2000)
ORDER BY distance_meters ASC
LIMIT 10;
```

### Execution Metrics

    1.  Total Latency: 500.33 ms

    2.  JIT Compilation Overhead: 244.18 ms

    3.  Scan Type: Parallel Sequential Scan (loops=2, 1 worker + leader)

    4.  Buffer I/O: shared hit=2945 read=3 dirtied=1 (~23.5 MB)

    5.  Rows Filtered (Brute Force): 94,388 rows rejected via CPU geodesic calculations

## 2. Optimized Spatial Search (GiST Index)
### Index Definition
```sql
CREATE INDEX idx_businesses_location_gist ON businesses USING GIST(location);
``` 


### Execution Metrics

    1.  Total Latency: 88.69 ms

    2.  JIT Compilation Overhead: 0.00 ms (Bypassed; cost dropped under jit_above_cost)

    3.  Scan Type: Bitmap Index Scan + Parallel Bitmap Heap Scan (loops=2)      
    
    4.  Index Traversal Time: 4.34 ms (122 buffer hits)

    5.  Buffer I/O: shared hit=2990

    6.  Rows Filtered (Exact Filter): 4,046 rows (down from 94,388 — a 95.7% reduction in CPU math)

### Architectural Breakdown
   
    ## Why Baseline Failed (Brute-Force Sequential Scan)
   
        1.  CPU Saturation from Geodesic Math: Calculating the distance between two points on an oblate spheroid (WGS 84 ellipsoid) requires Vincenty or Great-Circle trigonometric functions. Without an index, PostgreSQL executed these floating-point calculations for all 100,000 rows across every heap page.

        2.  JIT Compilation Bottleneck: Because the calculated planner cost exceeded 738,000 (well above PostgreSQL's default jit_above_cost threshold of 100,000), the engine invoked LLVM to compile expressions into native machine code. This compilation phase added an immediate 244.18 ms penalty before a single row was read.

        3.  Unbounded Sort Stage: All matching rows had to be gathered across parallel workers and sorted in a Top-N heapsort before returning the 10 closest matches.

### How GiST Solved It (Two-Phase Indexing)

        1.  Phase 1: Bounding-Box Pruning (Lossy Index Traversal): GiST structures spatial points into a hierarchical R-Tree of Minimum Bounding Rectangles (MBRs). PostgreSQL expanded the query center by 2,000 meters into a bounding rectangle (_st_expand) and evaluated intersection using the && operator. In just 4.34 ms, it narrowed 100,000 rows down to 9,659 candidates.

        2.  Phase 2: Exact Geodesic Recheck: Only the candidates that passed bounding-box intersection were evaluated with the precise ST_DWithin formula, eliminating over 95% of the computational workload.

        3.  JIT Elimination: The lower estimated planner cost (75,159) stayed below the JIT threshold, eliminating the 244 ms LLVM compilation step entirely.