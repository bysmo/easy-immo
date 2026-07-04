package com.easyimmo.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AgencyUpdateRequest {

    @NotBlank(message = "Le nom de l'agence est obligatoire")
    private String name;

    private String address;

    @NotBlank(message = "La ville est obligatoire")
    private String city;

    @NotBlank(message = "Le pays est obligatoire")
    @Pattern(regexp = "^(BJ|BF|CI|GW|ML|NE|SN|TG)$", message = "Pays UEMOA invalide")
    private String country;

    @NotBlank(message = "Le téléphone est obligatoire")
    private String phone;
}
