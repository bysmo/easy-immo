package com.easyimmo.property.dto;

import com.easyimmo.property.model.Property;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PropertyRequest {

    private String reference;

    @NotNull(message = "Le type de bien est obligatoire")
    private Property.PropertyType type;

    @NotBlank(message = "L'adresse est obligatoire")
    private String address;

    @NotBlank(message = "La ville est obligatoire")
    private String city;

    @Pattern(regexp = "^(BJ|BF|CI|GW|ML|NE|SN|TG)$", message = "Pays UEMOA invalide")
    private String country;

    @Min(value = 1, message = "Le nombre de pièces doit être au moins 1")
    private Integer rooms;

    private Integer bathrooms;

    @DecimalMin(value = "0.0", inclusive = false, message = "La surface doit être positive")
    private BigDecimal areaSqm;

    private Integer floor;

    @NotNull(message = "Le loyer est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le loyer doit être positif")
    private BigDecimal currentRent;

    @Min(value = 1, message = "La caution doit être d'au moins 1 mois")
    @Max(value = 12, message = "La caution ne peut pas dépasser 12 mois")
    private Integer depositMonths = 2;

    private UUID ownerId;

    private String description;

    // JSON array en string : ["parking","gardien","climatisation"]
    private String amenities;
}
