package com.yelp.business.service;

import com.yelp.business.dto.BusinessSearchResult;
import com.yelp.business.repository.BusinessRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class BusinessService
{
    private final BusinessRepository businessRepository;

    public BusinessService(BusinessRepository businessRepository)
    {
        this.businessRepository = businessRepository;
    }
    Page<BusinessSearchResult> searchBusinesses(
            double latitude,
            double longitude,
            double radiusMeters,
            String category,
            String query,
            int page,
            int size
    )
    {
        Pageable pageable = PageRequest.of(page, size);

        String cleanCategory = (category != null && !category.isBlank()) ? category.trim() : null;
        String cleanQuery = (query != null && !query.isBlank()) ? query.trim() : null;

        return businessRepository.searchNearBy(
                latitude,
                longitude,
                radiusMeters,
                cleanCategory,
                cleanQuery,
                pageable
        );
    }
}
