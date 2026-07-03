package com.easyimmo.tenant.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class AgencyCreateRequest {

    @NotBlank(message = "Le nom de l'agence est obligatoire")
    @Size(max = 200)
    private String name;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Numéro de téléphone invalide")
    private String phone;

    private String address;

    @NotBlank(message = "La ville est obligatoire")
    private String city;

    @NotBlank(message = "Le pays est obligatoire")
    @Pattern(regexp = "^(BJ|BF|CI|GW|ML|NE|SN|TG)$", message = "Pays UEMOA invalide")
    private String country;

    private UUID subscriptionPlanId;

    // Admin de l'agence (créé dans Keycloak)
    @NotBlank(message = "Le prénom de l'admin est obligatoire")
    private String adminFirstName;

    @NotBlank(message = "Le nom de l'admin est obligatoire")
    private String adminLastName;

    @NotBlank(message = "L'email de l'admin est obligatoire")
    @Email
    private String adminEmail;

    @NotBlank(message = "Le téléphone de l'admin est obligatoire")
    private String adminPhone;
}
