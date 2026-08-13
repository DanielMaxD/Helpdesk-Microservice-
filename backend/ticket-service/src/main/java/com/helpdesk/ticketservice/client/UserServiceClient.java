package com.helpdesk.ticketservice.client;

import com.helpdesk.ticketservice.dto.UserDto;
import com.helpdesk.ticketservice.exception.BadRequestException;
import com.helpdesk.ticketservice.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Thin REST client to user-service. Ticket-service never touches user-service's
 * database directly - every check goes through its public HTTP API, forwarding
 * the caller's own bearer token so user-service can apply its own authorization
 * rules (e.g. GET /api/users/{id} requires ADMIN or the user themself).
 */
@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(@Value("${user-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Used during ticket assignment to verify the target user exists, and by
     * TicketService callers who already hold a valid bearer token to forward.
     */
    public UserDto getUserById(UUID userId, String authorizationHeader) {
        try {
            return restClient.get()
                    .uri("/api/users/{id}", userId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(UserDto.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new BadRequestException("The selected agent does not exist");
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new BadRequestException("Not authorized to verify the selected agent");
        } catch (HttpClientErrorException ex) {
            throw new BadRequestException("Unable to verify the selected agent");
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException(
                    "The user service is temporarily unavailable. Please try again later.");
        }
    }

    /**
     * Used only by TicketDataSeeder at startup, to obtain a token for seeding
     * demo tickets against the real user-service demo accounts. Never used on
     * the request path - real requests always forward the caller's own token.
     */
    public Optional<String> login(String email, String password) {
        try {
            Map<String, String> body = Map.of("email", email, "password", password);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Object token = response != null ? response.get("token") : null;
            return Optional.ofNullable(token).map(Object::toString);
        } catch (RestClientException ex) {
            return Optional.empty();
        }
    }

    /**
     * Used only by TicketDataSeeder to look up the seeded demo users' real
     * UUIDs so seed tickets reference accounts that actually exist.
     */
    public List<UserDto> listAllUsers(String bearerToken) {
        try {
            UserDto[] users = restClient.get()
                    .uri("/api/users")
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .body(UserDto[].class);
            return users != null ? Arrays.asList(users) : List.of();
        } catch (RestClientException ex) {
            return List.of();
        }
    }
}
