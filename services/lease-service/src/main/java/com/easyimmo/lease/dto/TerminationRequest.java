package com.easyimmo.lease.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TerminationRequest {

    @NotNull(message = "La date de résiliation effective est obligatoire")
    private LocalDate terminationDate;

    @NotBlank(message = "Le motif de résiliation est obligatoire")
    private String reason;

    @NotNull(message = "Le montant de la caution restitué est obligatoire")
    @DecimalMin(value = "0.0", message = "Le montant doit être positif")
    private BigDecimal depositRefunded = BigDecimal.ZERO;

    private String notes;
}
