package com.easyimmo.lease.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class LeaseRequest {

    @NotNull(message = "L'ID du bien immobilier est obligatoire")
    private UUID propertyId;

    @NotNull(message = "L'ID du locataire est obligatoire")
    private UUID tenantId;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "Le montant du loyer est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le loyer doit être supérieur à 0")
    private BigDecimal monthlyRent;

    @NotNull(message = "Le montant de la caution est obligatoire")
    @DecimalMin(value = "0.0", message = "La caution doit être positive")
    private BigDecimal depositAmount;

    @Min(value = 1, message = "Le jour de paiement doit être entre 1 et 28")
    @Max(value = 28, message = "Le jour de paiement doit être entre 1 et 28")
    private Integer paymentDay = 5;

    private String notes;
}
