package com.easyimmo.lease.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RenewalRequest {

    @NotNull(message = "La nouvelle date de fin de bail est obligatoire")
    private LocalDate newEndDate;

    @NotNull(message = "Le nouveau montant du loyer est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le loyer doit être supérieur à 0")
    private BigDecimal newMonthlyRent;

    private String notes;
}
