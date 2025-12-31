package org.example.service;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.example.clients.UsersClient;
import org.example.exceptions.*;
import org.example.models.Member;
import org.example.models.MemberId;
import org.example.models.Organization;
import org.example.repositories.MembersRepository;
import org.example.repositories.OrganizationsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrganizationMembersService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationMembersService.class);

    private static final Marker MEMBERS_GET = MarkerFactory.getMarker("ORG_MEMBERS_GET");
    private static final Marker MEMBER_ADD = MarkerFactory.getMarker("ORG_MEMBER_ADD");
    private static final Marker MEMBER_REMOVE = MarkerFactory.getMarker("ORG_MEMBER_REMOVE");
    private static final Marker PERMISSION = MarkerFactory.getMarker("ORG_PERMISSION");

    @Autowired
    private OrganizationsRepository organizationsRepository;

    @Autowired
    private MembersRepository membersRepository;

    @Autowired
    private UsersClient usersClient;

    /**
     * Verifies if the requester has permission to manage members of the organization.
     *
     * Current rule:
     * - requester must be the creator of the organization (org.createdBy == requesterId).
     *
     * @param org organization to validate ownership against
     * @param requesterId requester identifier (typically the user performing the action)
     * @throws PermissionDeniedException if requester is null, org createdBy is null, or requester is not the creator
     */
    private void checkOwnerPermission(Organization org, UUID requesterId) {
        if (org.getCreatedBy() == null || requesterId == null) {
            logger.warn(PERMISSION, "Permission check failed due to missing ids (orgId={}, orgCreatedBy={}, requesterId={})",
                    org.getId(), org.getCreatedBy(), requesterId);
            throw new PermissionDeniedException("User is not the creator of the organization");
        }

        if (!org.getCreatedBy().equals(requesterId)) {
            logger.warn(PERMISSION, "Permission denied (orgId={}, orgCreatedBy={}, requesterId={})",
                    org.getId(), org.getCreatedBy(), requesterId);
            throw new PermissionDeniedException("User is not the creator of the organization");
        }

        logger.debug(PERMISSION, "Permission granted (orgId={}, requesterId={})", org.getId(), requesterId);
    }

    /**
     * Retrieves all members of a given organization.
     *
     * @param orgId organization identifier
     * @return list of members belonging to the organization
     * @throws OrganizationNotFoundException if the organization does not exist
     */
    public List<Member> getMembers(UUID orgId) {
        logger.debug(MEMBERS_GET, "Get members requested (orgId={})", orgId);

        if (!organizationsRepository.existsById(orgId)) {
            logger.warn(MEMBERS_GET, "Organization not found (orgId={})", orgId);
            throw new OrganizationNotFoundException("Organization not found");
        }

        List<Member> members = membersRepository.findByOrganization_Id(orgId);
        logger.debug(MEMBERS_GET, "Get members completed (orgId={}, results={})", orgId, members.size());

        return members;
    }

    /**
     * Adds a user as a member of an organization.
     *
     * Business rules:
     * - organization must exist
     * - requester must be the organization creator
     * - organization must be active
     * - user must exist in Users service
     * - if user is already a member, returns the existing member instead of creating a duplicate
     *
     * @param orgId organization identifier
     * @param userId user identifier to be added as a member
     * @param requesterId requester identifier (must be org creator)
     * @return persisted member (or existing member if already present)
     * @throws OrganizationNotFoundException if the organization does not exist
     * @throws PermissionDeniedException if requester lacks permissions or organization is inactive
     * @throws UserNotFoundException if userId is null or the user does not exist in Users service
     * @throws ExternalServiceException if Users service returns an error/unavailability (non-404)
     */
    @Transactional
    public Member addMember(UUID orgId, UUID userId, UUID requesterId) {
        logger.info(MEMBER_ADD, "Add member requested (orgId={}, userId={}, requesterId={})",
                orgId, userId, requesterId);

        Organization org = organizationsRepository.findById(orgId)
                .orElseThrow(() -> {
                    logger.warn(MEMBER_ADD, "Organization not found (orgId={})", orgId);
                    return new OrganizationNotFoundException("Organization not found");
                });

        checkOwnerPermission(org, requesterId);

        if (!org.isActive()) {
            logger.warn(MEMBER_ADD, "Cannot add member to inactive org (orgId={})", orgId);
            throw new PermissionDeniedException("Cannot add members to an inactive organization");
        }

        if (userId == null) {
            logger.warn(MEMBER_ADD, "User not found (userId=null, orgId={})", orgId);
            throw new UserNotFoundException("User not found");
        }

        try {
            usersClient.getUser(userId);
        } catch (FeignException.NotFound e) {
            logger.warn(MEMBER_ADD, "User not found in Users service (userId={}, orgId={})", userId, orgId);
            throw new UserNotFoundException("User not found");
        } catch (FeignException e) {
            logger.error(MEMBER_ADD, "Users service error (status={}, userId={}, orgId={})", e.status(), userId, orgId);
            throw new ExternalServiceException("Users service unavailable");
        }


        MemberId memberId = new MemberId(orgId, userId);

        if (membersRepository.existsById(memberId)) {
            logger.info(MEMBER_ADD, "User already member (orgId={}, userId={})", orgId, userId);
            return membersRepository.findById(memberId).get();
        }

        Member member = new Member();
        member.setId(memberId);
        member.setOrganization(org);
        member.setCreatedBy(requesterId);

        Member saved = membersRepository.save(member);

        logger.info(MEMBER_ADD, "Member added successfully (orgId={}, userId={})", orgId, userId);

        return saved;
    }

    /**
     * Removes a member from an organization.
     *
     * Business rules:
     * - organization must exist
     * - requester must be the organization creator
     * - member must exist
     *
     * @param orgId organization identifier
     * @param userId user identifier to be removed
     * @param requesterId requester identifier (must be org creator)
     * @throws OrganizationNotFoundException if the organization does not exist
     * @throws PermissionDeniedException if requester lacks permissions
     * @throws MemberNotFoundException if the member does not exist for the given orgId/userId pair
     */
    @Transactional
    public void removeMember(UUID orgId, UUID userId, UUID requesterId) {
        logger.info(MEMBER_REMOVE, "Remove member requested (orgId={}, userId={}, requesterId={})",
                orgId, userId, requesterId);

        Organization org = organizationsRepository.findById(orgId)
                .orElseThrow(() -> {
                    logger.warn(MEMBER_REMOVE, "Organization not found (orgId={})", orgId);
                    return new OrganizationNotFoundException("Organization not found");
                });

        checkOwnerPermission(org, requesterId);

        MemberId memberId = new MemberId(orgId, userId);

        Member member = membersRepository.findById(memberId)
                .orElseThrow(() -> {
                    logger.warn(MEMBER_REMOVE, "Member not found (orgId={}, userId={})", orgId, userId);
                    return new MemberNotFoundException("Member not found");
                });

        membersRepository.delete(member);

        logger.info(MEMBER_REMOVE, "Member removed successfully (orgId={}, userId={})", orgId, userId);
    }
}
