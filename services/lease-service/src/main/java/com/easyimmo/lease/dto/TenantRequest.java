package com.easyimmo.lease.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantRequest {

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Numéro de téléphone invalide")
    private String phone;

    @Email(message = "Email invalide")
    private String email;

    private String nationalId;
    private String profession;

    private String emergencyContactName;

    // Contact urgence facultatif — validation seulement si non vide
    @Pattern(regexp = "^(\\+?[0-9]{8,15})?$", message = "Numéro de contact d'urgence invalide")
    private String emergencyContactPhone;

    private String keycloakUserId;
}
