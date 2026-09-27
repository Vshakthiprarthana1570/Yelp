package com.yelp.business.controller;

import com.yelp.business.dto.BusinessSearchResult;
import com.yelp.business.service.BusinessService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses")
@Validated
public class BusinessController
{
    private final BusinessService businessService;

    public BusinessController(BusinessService businessService)
    {
        this.businessService = businessService;
    }

    @GetMapping
    public ResponseEntity<Page<BusinessSearchResult>> search
            (
                    @RequestParam @DecimalMin("90.0") @DecimalMax("90.0") double latitude,
                    @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
                    @RequestParam(defaultValue = "5000") double radiusMeters,
                    @RequestParam(required = false) String category,
                    @RequestParam(required = false) String query,
                    @RequestParam(defaultValue = "0") @Min(0) int page,
                    @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
            )
    {
        Page<BusinessSearchResult> results = businessService.searchBusinesses(latitude, longitude, radiusMeters, category, query, page, size);
        return ResponseEntity.ok(results);
    }

}