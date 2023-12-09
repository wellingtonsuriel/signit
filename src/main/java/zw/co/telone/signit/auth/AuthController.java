package zw.co.telone.signit.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import zw.co.telone.signit.config.JwtUtil;

import java.util.HashMap;
import java.util.Map;



@CrossOrigin("*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;

    @Autowired
    public AuthController(JwtUtil jwtUtil, RestTemplate restTemplate) {
        this.jwtUtil = jwtUtil;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<Map<String, Object>> authenticate(@RequestBody AuthenticationRequest authenticationRequest)  throws JsonProcessingException {
        String url = "http://careers.telone.co.zw:1930/ActiveDirectoryLogin/api/v1/ldap/auth";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthenticationRequest> request = new HttpEntity<>(authenticationRequest, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        // Parse the response body as JSON
        JsonNode responseJson = objectMapper.readTree(response.getBody());

// Extract the boolean value from the "auth" field
        boolean isAuthenticated = responseJson.get("auth").asBoolean();

        if (isAuthenticated) {
            // Authentication successful
            String authToken = jwtUtil.generateToken(authenticationRequest.getUsername());
            //return ResponseEntity.ok(authToken);
           // return ResponseEntity.ok().body(Map.of("token", authToken));
            Map<String, Object> response1 = new HashMap<>();
            response1.put("Token", authToken);

            // Return the JSON response using ResponseEntity.ok()
            return ResponseEntity.ok(response1);
        } else {
            // Authentication failed
            return ResponseEntity.ok().body(Map.of("response", "Incorrect username and password"));

        }
    }
}