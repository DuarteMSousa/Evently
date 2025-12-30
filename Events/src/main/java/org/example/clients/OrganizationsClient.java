package org.example.clients;

import org.example.dtos.externalServices.organizations.OrganizationDTO;
import org.example.dtos.externalServices.venues.VenueDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "organizations", path = "/organizations")
public interface OrganizationsClient {

    @GetMapping("/get-organization/{id}")
    ResponseEntity<OrganizationDTO> getOrganization(@PathVariable("id") UUID id);


}
