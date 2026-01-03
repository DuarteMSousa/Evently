package org.example.clients;

import org.example.dtos.UserDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@FeignClient(name = "users", path = "/users")
public interface UsersClient {

    @GetMapping("/get-user/{id}")
    ResponseEntity<UserDTO> getUser(@PathVariable("id") UUID id);

}