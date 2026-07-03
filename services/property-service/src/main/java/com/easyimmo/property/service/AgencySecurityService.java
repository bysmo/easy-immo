package com.easyimmo.property.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("agencySecurityService")
public class AgencySecurityService {

    /**
     * Extrait l'agency_id du JWT et le compare à l'ID de l'agence demandé
     */
    public boolean checkAgency(UUID requestedAgencyId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            return false;
        }
        
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String jwtAgencyId = jwt.getClaimAsString("agency_id");
        
        if (jwtAgencyId == null) {
            return false;
        }
        
        return jwtAgencyId.equalsIgnoreCase(requestedAgencyId.toString());
    }

    /**
     * Extrait l'agency_id du JWT
     */
    public UUID getAgencyId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new IllegalArgumentException("Utilisateur non authentifié avec JWT");
        }
        
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String jwtAgencyId = jwt.getClaimAsString("agency_id");
        
        if (jwtAgencyId == null) {
            throw new IllegalArgumentException("Token JWT manquant le claim 'agency_id'");
        }
        
        return UUID.fromString(jwtAgencyId);
    }

    /**
     * Extrait l'ID utilisateur (subject) du JWT
     */
    public UUID getUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            return null;
        }
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return UUID.fromString(jwt.getSubject());
    }
}
