package com.yelp.business.dto;

import java.math.BigDecimal;

public interface BusinessSearchResult
{
    Long getId();
    String getName();
    String getCategory();
    String getAddress();
    Double getLatitude();
    Double getLongitude();
    BigDecimal getAvgRating();
    Integer getNumRatings();
    Double getDistanceMeters();
}
