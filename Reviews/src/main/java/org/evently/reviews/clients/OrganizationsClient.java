package org.evently.reviews.clients;

import org.evently.reviews.dtos.externalServices.organizations.OrganizationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "organizations", path = "/organizations")
public interface OrganizationsClient {

    @GetMapping("/get-organization/{orgId}")
    ResponseEntity<OrganizationDTO> getOrganization(@PathVariable("orgId") UUID orgId);

}
