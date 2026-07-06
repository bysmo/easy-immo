package com.easyimmo.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class KeycloakTest {

    @Test
    public void listKeycloakUsers() {
        System.out.println("=== KEYCLOAK USERS LISTING ===");
        RestTemplate restTemplate = new RestTemplate();
        String serverUrl = "http://localhost:8180";
        String realm = "easy-immo";
        
        try {
            // 1. Get Admin Token
            String tokenUrl = serverUrl + "/realms/master/protocol/openid-connect/token";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("grant_type", "password");
            map.add("client_id", "admin-cli");
            map.add("username", "admin");
            map.add("password", "admin");
            
            HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(map, headers);
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenUrl, tokenRequest, Map.class);
            String adminToken = (String) tokenResponse.getBody().get("access_token");
            System.out.println("Admin token successfully retrieved");
            
            // 2. Fetch Users
            String usersUrl = serverUrl + "/admin/realms/" + realm + "/users";
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(adminToken);
            HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);
            
            ResponseEntity<List> usersResponse = restTemplate.exchange(usersUrl, HttpMethod.GET, userRequest, List.class);
            List<Map<String, Object>> users = usersResponse.getBody();
            System.out.println("Found " + users.size() + " users:");
            for (Map<String, Object> user : users) {
                System.out.println("\nUser: " + user.get("username") 
                                   + " | ID: " + user.get("id") 
                                   + " | Email: " + user.get("email")
                                   + " | Enabled: " + user.get("enabled"));
                System.out.println("Attributes: " + user.get("attributes"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("=== END KEYCLOAK USERS LISTING ===");
    }
}
