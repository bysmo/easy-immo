package com.easyimmo.property.dto;

import com.easyimmo.property.model.PropertyRepair;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RepairRequest {

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @DecimalMin(value = "0.0", message = "Le coût estimé doit être positif")
    private BigDecimal estimatedCost;

    private BigDecimal actualCost;

    private LocalDate repairDate;

    private PropertyRepair.RepairStatus status = PropertyRepair.RepairStatus.PENDING;

    private PropertyRepair.RepairPayer payer;

    private String notes;
}
