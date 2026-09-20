# Yelp

A low-latency, read-heavy Yelp backend replica built around geospatial proximity queries, trigram text matching, and atomic review aggregation using PostgreSQL, PostGIS, and `pg_trgm`.

## Requirements

### Functional Requirements (FR)

1. **Business Search:** Users can search businesses by name/keyword (fuzzy matching), geographical location (`lat`, `lon`, `radius`), and category.
2. **Business Profile & Reviews:** Users can view business metadata along with its aggregated rating, total review count, and paginated review list.
3. **Submit / Update Review:** Authenticated users can submit a review (mandatory 1–5 integer rating and optional text).
   - **Constraint:** Exactly one active rating/review per user per business (subsequent submissions act as updates).

### Non-Functional Requirements (NFR)

1. **Low Read Latency:** Search and lookup operations must return in `< 500ms` (Target P99: `< 100ms`).
2. **High Availability & Eventual Consistency:** Read operations prioritize availability; slight aggregate metric propagation delays are acceptable.
3. **Scale Capacity:** Designed to support a footprint of 10M businesses and 100M Daily Active Users (~5,000–15,000 read QPS peak; ~10–30 write QPS).
4. **Data Integrity:** Strict enforcement of the single-vote invariant and prevention of floating-point rating drift under concurrent updates.
