package com.easyimmo.property.dto;

import com.easyimmo.property.model.Property;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PropertyResponse {
    private UUID id;
    private UUID agencyId;
    private String reference;
    private Property.PropertyType type;
    private String address;
    private String city;
    private String country;
    private Integer rooms;
    private Integer bathrooms;
    private BigDecimal areaSqm;
    private Integer floor;
    private BigDecimal currentRent;
    private Integer depositMonths;
    private BigDecimal depositAmount;       // currentRent * depositMonths
    private Property.PropertyStatus status;
    private String description;
    private String amenities;
    private UUID ownerId;
    private String ownerFullName;
    private BigDecimal ownerSharePercentage;
    private List<PhotoDto> photos;
    private String coverPhotoUrl;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class PhotoDto {
        private UUID id;
        private String url;
        private Boolean isCover;
        private Integer sortOrder;
    }
}
