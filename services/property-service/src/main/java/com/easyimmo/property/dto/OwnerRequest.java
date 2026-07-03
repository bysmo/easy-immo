package com.easyimmo.property.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OwnerRequest {

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Numéro invalide")
    private String phone;

    @Email(message = "Email invalide")
    private String email;

    private String nationalId;
    private String address;

    // Informations bancaires
    private String bankName;
    private String bankAccount;

    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Numéro mobile money invalide")
    private String mobileMoneyPhone;

    @Pattern(regexp = "^(mtn|orange)$", message = "Provider mobile money invalide (mtn ou orange)")
    private String mobileMoneyProvider;

    @DecimalMin(value = "0.0", inclusive = false, message = "Le pourcentage doit être positif")
    @DecimalMax(value = "100.0", message = "Le pourcentage ne peut pas dépasser 100%")
    private BigDecimal sharePercentage = BigDecimal.valueOf(80.00);

    private String notes;
}
