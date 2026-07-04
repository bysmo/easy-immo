package com.easyimmo.tenant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KeycloakService {

    @Value("${easy-immo.keycloak.server-url:http://localhost:8180}")
    private String serverUrl;

    @Value("${easy-immo.keycloak.realm:easy-immo}")
    private String realm;

    @Value("${easy-immo.keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${easy-immo.keycloak.admin-password:admin}")
    private String adminPassword;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Récupère un token d'accès administrateur de Keycloak sur le realm master
     */
    private String getAdminToken() {
        try {
            String tokenUrl = serverUrl + "/realms/master/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("grant_type", "password");
            map.add("client_id", "admin-cli");
            map.add("username", adminUsername);
            map.add("password", adminPassword);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible d'obtenir le token d'admin Keycloak");
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du token admin Keycloak", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de connexion Keycloak: " + e.getMessage());
        }
    }

    /**
     * Crée un utilisateur dans Keycloak avec le bon rôle et l'attribut agency_id
     * Retourne le Keycloak User ID (UUID)
     */
    public String createUser(String username, String email, String firstName, String lastName, 
                             String password, String agencyId, String roleName) {
        String adminToken = getAdminToken();

        // 1. Créer l'utilisateur
        String userId;
        try {
            String usersUrl = serverUrl + "/admin/realms/" + realm + "/users";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(adminToken);

            Map<String, Object> credential = Map.of(
                "type", "password",
                "value", password,
                "temporary", true
            );

            Map<String, Object> userPayload = Map.of(
                "username", username,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", true,
                "emailVerified", true,
                "attributes", Map.of("agency_id", List.of(agencyId)),
                "credentials", List.of(credential)
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(userPayload, headers);
            ResponseEntity<Void> response = restTemplate.postForEntity(usersUrl, request, Void.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                List<String> locations = response.getHeaders().get("Location");
                if (locations == null || locations.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Keycloak n'a pas retourné l'URI de l'utilisateur");
                }
                String location = locations.get(0);
                userId = location.substring(location.lastIndexOf("/") + 1);
                log.info("Utilisateur Keycloak créé avec ID: {}", userId);
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Échec création utilisateur Keycloak");
            }
        } catch (HttpClientErrorException.Conflict e) {
            log.warn("Conflit Keycloak (Email ou Username déjà utilisé) : {}", e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'identifiant ou l'email est déjà utilisé dans Keycloak");
        } catch (Exception e) {
            log.error("Erreur création utilisateur Keycloak", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur création Keycloak : " + e.getMessage());
        }

        // 2. Assigner le rôle (AGENCY_ADMIN ou AGENCY_AGENT)
        try {
            // A. Récupérer la représentation du rôle
            String roleUrl = serverUrl + "/admin/realms/" + realm + "/roles/" + roleName;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<Void> roleRequest = new HttpEntity<>(headers);
            
            ResponseEntity<Map> roleResponse = restTemplate.exchange(roleUrl, HttpMethod.GET, roleRequest, Map.class);
            if (!roleResponse.getStatusCode().is2xxSuccessful() || roleResponse.getBody() == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Rôle Keycloak introuvable: " + roleName);
            }
            Map roleRepresentation = roleResponse.getBody();

            // B. Assigner ce rôle à l'utilisateur
            String assignRoleUrl = serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";
            HttpEntity<List<Map>> assignRequest = new HttpEntity<>(List.of(roleRepresentation), headers);
            
            ResponseEntity<Void> assignResponse = restTemplate.postForEntity(assignRoleUrl, assignRequest, Void.class);
            if (!assignResponse.getStatusCode().is2xxSuccessful()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Échec association du rôle Keycloak");
            }
            log.info("Rôle {} assigné avec succès à l'utilisateur Keycloak {}", roleName, userId);
        } catch (Exception e) {
            log.error("Erreur assignation rôle Keycloak", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur assignation rôle Keycloak : " + e.getMessage());
        }

        return userId;
    }
}
