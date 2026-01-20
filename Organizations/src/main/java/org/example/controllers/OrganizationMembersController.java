package org.example.controllers;

import org.example.dtos.members.MemberDTO;
import org.example.exceptions.*;
import org.example.models.Member;
import org.example.service.OrganizationMembersService;
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
public class OrganizationMembersController {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationMembersController.class);

    private static final Marker ORG_MEMBERS_GET = MarkerFactory.getMarker("ORG_MEMBERS_GET");
    private static final Marker ORG_MEMBER_ADD = MarkerFactory.getMarker("ORG_MEMBER_ADD");
    private static final Marker ORG_MEMBER_REMOVE = MarkerFactory.getMarker("ORG_MEMBER_REMOVE");

    @Autowired
    private OrganizationMembersService organizationMembersService;

    private final ModelMapper modelMapper = new ModelMapper();

    private MemberDTO toMemberDTO(Member member) {
        MemberDTO dto = modelMapper.map(member, MemberDTO.class);
        dto.setOrganizationId(member.getId().getOrganizationId());
        dto.setUserId(member.getId().getUserId());
        return dto;
    }

    @GetMapping("/get-members/{orgId}")
    public ResponseEntity<?> getMembers(@PathVariable("orgId") UUID orgId) {
        /* HttpStatus(produces)
         * 200 OK - Members of the organization retrieved successfully.
         * 404 NOT_FOUND - Organization does not exist.
         * 400 BAD_REQUEST - Generic error.
         */
        logger.info(ORG_MEMBERS_GET, "GET /organizations/get-members/{} requested", orgId);

        try {
            List<MemberDTO> members = organizationMembersService.getMembers(orgId)
                    .stream()
                    .map(this::toMemberDTO)
                    .collect(Collectors.toList());

            logger.info(ORG_MEMBERS_GET, "Get members succeeded (orgId={}, results={})", orgId, members.size());

            return ResponseEntity.status(HttpStatus.OK).body(members);

        } catch (OrganizationNotFoundException e) {
            logger.warn(ORG_MEMBERS_GET, "Get members failed - organization not found (orgId={})", orgId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (Exception e) {
            logger.error(ORG_MEMBERS_GET, "Get members failed - unexpected error (orgId={})", orgId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/add-member/{orgId}/{userId}")
    public ResponseEntity<?> addMember(@PathVariable("orgId") UUID orgId,
                                       @PathVariable("userId") UUID userId,
                                       @RequestParam("requesterId") UUID requesterId) {
        /* HttpStatus(produces)
         * 201 CREATED - Member added successfully.
         * 403 FORBIDDEN - Permission denied (requester is not allowed to add members).
         * 404 NOT_FOUND - Organization does not exist.
         * 405 METHOD_NOT_ALLOWED - User does not exist.
         * 400 BAD_REQUEST - Generic error.
         */

        logger.info(ORG_MEMBER_ADD,
                "POST /organizations/add-member/{}/{} requested (requesterId={})",
                orgId, userId, requesterId);

        try {
            Member newMember = organizationMembersService.addMember(orgId, userId, requesterId);

            logger.info(ORG_MEMBER_ADD,
                    "Add member succeeded (orgId={}, userId={}, requesterId={})",
                    orgId, userId, requesterId);

            return ResponseEntity.status(HttpStatus.CREATED).body(toMemberDTO(newMember));

        } catch (PermissionDeniedException e) {
            logger.warn(ORG_MEMBER_ADD,
                    "Add member failed - permission denied (orgId={}, userId={}, requesterId={})",
                    orgId, userId, requesterId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());

        } catch (OrganizationNotFoundException e) {
            logger.warn(ORG_MEMBER_ADD, "Add member failed - organization not found (orgId={})", orgId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (MemberAlreadyExistsException e) {
            logger.warn(ORG_MEMBER_ADD,
                    "Add member failed - already member (orgId={}, userId={}, requesterId={})",
                    orgId, userId, requesterId);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());

        }catch (UserNotFoundException e) {
            logger.warn(ORG_MEMBER_ADD, "Add member failed - user not found (userId={})", userId);
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(e.getMessage());

        } catch (Exception e) {
            logger.error(ORG_MEMBER_ADD,
                    "Add member failed - unexpected error (orgId={}, userId={}, requesterId={})",
                    orgId, userId, requesterId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/remove-member/{orgId}/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable("orgId") UUID orgId,
                                          @PathVariable("userId") UUID userId,
                                          @RequestParam("requesterId") UUID requesterId) {
        /* HttpStatus(produces)
         * 200 OK - Member removed successfully.
         * 403 FORBIDDEN - Permission denied (requester is not allowed to remove members).
         * 404 NOT_FOUND - Organization does not exist.
         * 405 METHOD_NOT_ALLOWED - Member does not exist in the organization.
         * 400 BAD_REQUEST - Generic error.
         */

        logger.info(ORG_MEMBER_REMOVE,
                "DELETE /organizations/remove-member/{}/{} requested (requesterId={})",
                orgId, userId, requesterId);

        try {
            organizationMembersService.removeMember(orgId, userId, requesterId);

            logger.info(ORG_MEMBER_REMOVE,
                    "Remove member succeeded (orgId={}, userId={}, requesterId={})",
                    orgId, userId, requesterId);

            return ResponseEntity.status(HttpStatus.OK).body("Member removed");

        } catch (PermissionDeniedException e) {
            logger.warn(ORG_MEMBER_REMOVE,
                    "Remove member failed - permission denied (orgId={}, userId={}, requesterId={})",
                    orgId, userId, requesterId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());

        } catch (OrganizationNotFoundException e) {
            logger.warn(ORG_MEMBER_REMOVE, "Remove member failed - organization not found (orgId={})", orgId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (MemberNotFoundException e) {
            logger.warn(ORG_MEMBER_REMOVE, "Remove member failed - member not found (orgId={}, userId={})", orgId, userId);
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(e.getMessage());

        } catch (Exception e) {
            logger.error(ORG_MEMBER_REMOVE,
                    "Remove member failed - unexpected error (orgId={}, userId={}, requesterId={})",
                    orgId, userId, requesterId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

}
