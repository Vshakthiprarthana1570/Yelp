package com.yelp.business.repository;

import com.yelp.business.dto.BusinessSearchResult;
import com.yelp.business.model.Business;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessRepository extends JpaRepository<Business, Long>
{
    @SuppressWarnings("SqlResolve")
    @Query(value = """
            SELECT b.id as id,
                    b.name as name,
                    b.category as category,
                    b.address as address,
                    b.latitude as latitude,
                    b.longitude as longitude,
                    b.avg_rating AS avgRating,
                    b.num_ratings AS numRatings,
                    ST_Distance(b.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) AS distanceMeters 
                                FROM businesses b WHERE ST_DWithin(b.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters)
                        AND (:category IS NULL OR b.category = :category)
                        AND (:query IS NULL OR b.name ILIKE CONCAT('%', :query, '%'))
            ORDER BY distanceMeters ASC
            """,
            countQuery = """
            SELECT count(b.id)
            FROM businesses b
            WHERE 
                ST_DWithin(
                    b.location, 
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, 
                    :radiusMeters
                )
                AND (:category IS NULL OR b.category = :category)
                AND (:query IS NULL OR b.name ILIKE CONCAT('%', :query, '%'))
            """, nativeQuery = true)
 Page<BusinessSearchResult> searchNearBy(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters,
            @Param("category") String category,
            @Param("query") String query,
            Pageable pageable
    );
}
