package org.evently.clients;

import org.evently.dtos.externalServices.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "users", path = "/users")
public interface UsersClient {

    @GetMapping("/get-user/{id}")
    ResponseEntity<UserDTO> getUser(@PathVariable("id") UUID id);
}
