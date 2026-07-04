package com.easyimmo.tenant.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("agencySecurityService")
public class AgencySecurityService {

    public boolean isSameAgency(UUID agencyId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            return false;
        }
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String tokenAgencyId = jwt.getClaimAsString("agency_id");
        return tokenAgencyId != null && tokenAgencyId.equalsIgnoreCase(agencyId.toString());
    }

    public UUID getAgencyId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Non authentifié ou token invalide");
        }
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String tokenAgencyId = jwt.getClaimAsString("agency_id");
        if (tokenAgencyId == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "L'attribut d'agence (agency_id) est manquant dans le token");
        }
        return UUID.fromString(tokenAgencyId);
    }
}
