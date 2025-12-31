package org.example.controllers;

import org.example.dtos.organizations.OrganizationCreateDTO;
import org.example.dtos.organizations.OrganizationDTO;
import org.example.dtos.organizations.OrganizationUpdateDTO;
import org.example.exceptions.InvalidOrganizationException;
import org.example.exceptions.OrganizationNotFoundException;
import org.example.exceptions.PermissionDeniedException;
import org.example.models.Organization;
import org.example.service.OrganizationsService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(OrganizationsController.class);

    private static final Marker ORGS_LIST = MarkerFactory.getMarker("ORGS_LIST");
    private static final Marker ORG_GET = MarkerFactory.getMarker("ORG_GET");
    private static final Marker ORG_CREATE = MarkerFactory.getMarker("ORG_CREATE");
    private static final Marker ORG_UPDATE = MarkerFactory.getMarker("ORG_UPDATE");

    @Autowired
    private OrganizationsService organizationsService;

    private final ModelMapper modelMapper = new ModelMapper();

    private OrganizationDTO toOrganizationDTO(Organization org) {
        return modelMapper.map(org, OrganizationDTO.class);
    }

    @GetMapping("/get-organizations")
    public ResponseEntity<?> getOrganizations() {
        /* HttpStatus(produces)
         * 200 OK - Organizations retrieved successfully.
         * 400 BAD_REQUEST - Generic error.
         */
        logger.info(ORGS_LIST, "GET /organizations/get-organizations requested");

        try {
            List<OrganizationDTO> orgs = organizationsService.getOrganizations()
                    .stream()
                    .map(this::toOrganizationDTO)
                    .collect(Collectors.toList());

            logger.info(ORGS_LIST, "Get organizations succeeded (results={})", orgs.size());

            return ResponseEntity.status(HttpStatus.OK).body(orgs);

        } catch (Exception e) {
            logger.error(ORGS_LIST, "Get organizations failed - unexpected error", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/get-organization/{orgId}")
    public ResponseEntity<?> getOrganization(@PathVariable("orgId") UUID orgId) {
        /* HttpStatus(produces)
         * 200 OK - Organization found.
         * 404 NOT_FOUND - Organization does not exist.
         * 400 BAD_REQUEST - Generic error.
         */
        logger.info(ORG_GET, "GET /organizations/get-organization/{} requested", orgId);

        try {
            Organization org = organizationsService.getOrganization(orgId);

            logger.info(ORG_GET, "Get organization succeeded (orgId={}, name={})", org.getId(), org.getName());

            return ResponseEntity.status(HttpStatus.OK).body(toOrganizationDTO(org));

        } catch (OrganizationNotFoundException e) {
            logger.warn(ORG_GET, "Get organization failed - not found (orgId={})", orgId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (Exception e) {
            logger.error(ORG_GET, "Get organization failed - unexpected error (orgId={})", orgId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/create-organization")
    public ResponseEntity<?> createOrganization(@RequestBody OrganizationCreateDTO dto) {
        /* HttpStatus(produces)
         * 201 CREATED - Organization created successfully.
         * 400 BAD_REQUEST - Invalid data provided / generic error.
         */
        logger.info(ORG_CREATE, "POST /organizations/create-organization requested (name={})", dto.getName());

        try {
            Organization org = modelMapper.map(dto, Organization.class);
            Organization newOrg = organizationsService.createOrganization(org);

            logger.info(ORG_CREATE, "Create organization succeeded (orgId={}, name={})", newOrg.getId(), newOrg.getName());

            return ResponseEntity.status(HttpStatus.CREATED).body(toOrganizationDTO(newOrg));

        } catch (InvalidOrganizationException e) {
            logger.warn(ORG_CREATE, "Create organization failed - invalid payload (name={}) reason={}",
                    dto.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            logger.error(ORG_CREATE, "Create organization failed - unexpected error (name={})", dto.getName(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/update-organization/{orgId}")
    public ResponseEntity<?> updateOrganization(@PathVariable("orgId") UUID orgId,
                                                @RequestBody OrganizationUpdateDTO dto) {
        /* HttpStatus(produces)
         * 200 OK - Organization updated successfully.
         * 403 FORBIDDEN - Permission denied (requester is not allowed to update organization).
         * 404 NOT_FOUND - Organization does not exist.
         * 400 BAD_REQUEST - Invalid data provided / generic error.
         */

        logger.info(ORG_UPDATE, "PUT /organizations/update-organization/{} requested", orgId);

        try {
            Organization orgWithUpdates = modelMapper.map(dto, Organization.class);
            Organization updated = organizationsService.updateOrganization(orgId, orgWithUpdates);

            logger.info(ORG_UPDATE, "Update organization succeeded (orgId={}, name={})",
                    updated.getId(), updated.getName());

            return ResponseEntity.status(HttpStatus.OK).body(toOrganizationDTO(updated));

        } catch (InvalidOrganizationException e) {
            logger.warn(ORG_UPDATE, "Update organization failed - invalid payload (orgId={}) reason={}",
                    orgId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (PermissionDeniedException e) {
            logger.warn(ORG_UPDATE, "Update organization failed - permission denied (orgId={})", orgId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());

        } catch (OrganizationNotFoundException e) {
            logger.warn(ORG_UPDATE, "Update organization failed - not found (orgId={})", orgId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (Exception e) {
            logger.error(ORG_UPDATE, "Update organization failed - unexpected error (orgId={})", orgId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<?> getOrganizationsByUser(@PathVariable("userId") UUID userId) {
        /* HttpStatus(produces)
         * 200 OK - List of organizations for the specified user retrieved successfully.
         * 404 NOT_FOUND - User not found or user has no organizations.
         * 400 BAD_REQUEST - Generic error.
         */

        logger.info(ORGS_LIST, "GET /organizations/by-user/{} requested", userId);

        try {
            List<OrganizationDTO> orgs = organizationsService.getOrganizationsByUser(userId)
                    .stream()
                    .map(this::toOrganizationDTO)
                    .collect(Collectors.toList());

            logger.info(ORGS_LIST, "200 OK returned, organizations by user retrieved (userId={}, results={})",
                    userId, orgs.size());

            return ResponseEntity.ok(orgs);

        } catch (OrganizationNotFoundException e) {
            logger.warn(ORGS_LIST, "404 NOT_FOUND: No organizations found for user (userId={})", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (Exception e) {
            logger.error(ORGS_LIST, "400 BAD_REQUEST: Exception caught while getting organizations by user: {}",
                    e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
