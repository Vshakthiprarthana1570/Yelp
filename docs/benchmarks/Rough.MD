Without Indexing   

QUERY PLAN

---------------------------------------------------------------------------------------------------------------------------------------------------
Limit  (cost=739348.44..739349.13 rows=6 width=53) (actual time=312.056..329.962 rows=10 loops=1)
Buffers: shared hit=2945 read=3 dirtied=1
->  Gather Merge  (cost=739348.44..739349.13 rows=6 width=53) (actual time=163.562..181.466 rows=10 loops=1)
Workers Planned: 1
Workers Launched: 1
Buffers: shared hit=2945 read=3 dirtied=1
->  Sort  (cost=738348.43..738348.45 rows=6 width=53) (actual time=147.222..147.223 rows=6 loops=2)
Sort Key: (st_distance(location, '0101000020E610000050FC1873D79A5EC0D0D556EC2FE34240'::geography, true))
Sort Method: top-N heapsort  Memory: 27kB
Buffers: shared hit=2945 read=3 dirtied=1
Worker 0:  Sort Method: top-N heapsort  Memory: 27kB
->  Parallel Seq Scan on businesses  (cost=0.00..738348.35 rows=6 width=53) (actual time=73.159..146.368 rows=2806 loops=2)
Filter: st_dwithin(location, '0101000020E610000050FC1873D79A5EC0D0D556EC2FE34240'::geography, '2000'::double precision, true)
Rows Removed by Filter: 47194
Buffers: shared hit=2908 read=3 dirtied=1
Planning:
Buffers: shared hit=178
Planning Time: 7.028 ms
JIT:
Functions: 9
Options: Inlining true, Optimization true, Expressions true, Deforming true
Timing: Generation 1.355 ms, Inlining 121.315 ms, Optimization 62.747 ms, Emission 58.761 ms, Total 244.178 ms
Execution Time: 500.332 ms
(23 rows)




With Indexing

                                                                          QUERY PLAN                                                                           
---------------------------------------------------------------------------------------------------------------------------------------------------------------
Limit  (cost=76159.16..76159.85 rows=6 width=53) (actual time=81.260..88.098 rows=10 loops=1)
Buffers: shared hit=2990
->  Gather Merge  (cost=76159.16..76159.85 rows=6 width=53) (actual time=81.257..88.092 rows=10 loops=1)
Workers Planned: 1
Workers Launched: 1
Buffers: shared hit=2990
->  Sort  (cost=75159.15..75159.16 rows=6 width=53) (actual time=66.864..66.867 rows=8 loops=2)
Sort Key: (st_distance(location, '0101000020E610000050FC1873D79A5EC0D0D556EC2FE34240'::geography, true))
Sort Method: top-N heapsort  Memory: 27kB
Buffers: shared hit=2990
Worker 0:  Sort Method: top-N heapsort  Memory: 27kB
->  Parallel Bitmap Heap Scan on businesses  (cost=422.06..75159.07 rows=6 width=53) (actual time=25.725..64.409 rows=2806 loops=2)
Filter: st_dwithin(location, '0101000020E610000050FC1873D79A5EC0D0D556EC2FE34240'::geography, '2000'::double precision, true)
Rows Removed by Filter: 2023
Heap Blocks: exact=1644
Buffers: shared hit=2982
->  Bitmap Index Scan on idx_businesses_location_gist  (cost=0.00..422.06 rows=9821 width=0) (actual time=4.342..4.342 rows=9659 loops=1)
Index Cond: (location && _st_expand('0101000020E610000050FC1873D79A5EC0D0D556EC2FE34240'::geography, '2000'::double precision))
Buffers: shared hit=122
Planning:
Buffers: shared hit=221
Planning Time: 26.579 ms
Execution Time: 88.690 ms
(23 rows)
