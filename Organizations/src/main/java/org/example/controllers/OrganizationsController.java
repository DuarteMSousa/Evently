package org.example.controllers;


import org.example.dtos.organizations.OrganizationCreateDTO;
import org.example.dtos.organizations.OrganizationDTO;
import org.example.dtos.organizations.OrganizationUpdateDTO;
import org.example.exceptions.*;
import org.example.models.Organization;
import org.example.service.OrganizationsService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/organizations")
public class OrganizationsController {

    @Autowired
    private OrganizationsService organizationsService;

    private final ModelMapper modelMapper = new ModelMapper();

    private OrganizationDTO toOrganizationDTO(Organization org) {
        return modelMapper.map(org, OrganizationDTO.class);
    }

    @GetMapping("/get-organizations")
    public ResponseEntity<?> getOrganizations() {
        try {
            List<OrganizationDTO> orgs = organizationsService.getOrganizations()
                    .stream()
                    .map(this::toOrganizationDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK).body(orgs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/get-organization/{orgId}")
    public ResponseEntity<?> getOrganization(@PathVariable("orgId") UUID orgId) {
        try {
            Organization org = organizationsService.getOrganization(orgId);
            return ResponseEntity.status(HttpStatus.OK).body(toOrganizationDTO(org));
        } catch (OrganizationNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/create-organization")
    public ResponseEntity<?> createOrganization(@RequestBody OrganizationCreateDTO dto) {
        try {
            Organization org = modelMapper.map(dto, Organization.class);
            Organization newOrg = organizationsService.createOrganization(org);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toOrganizationDTO(newOrg));
        } catch (InvalidOrganizationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/update-organization/{orgId}")
    public ResponseEntity<?> updateOrganization(@PathVariable("orgId") UUID orgId,
                                                @RequestBody OrganizationUpdateDTO dto) {
        try {
            Organization orgWithUpdates = modelMapper.map(dto, Organization.class);
            Organization updated = organizationsService.updateOrganization(orgId, orgWithUpdates);

            return ResponseEntity.status(HttpStatus.OK).body(toOrganizationDTO(updated));
        } catch (InvalidOrganizationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (PermissionDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (OrganizationNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
