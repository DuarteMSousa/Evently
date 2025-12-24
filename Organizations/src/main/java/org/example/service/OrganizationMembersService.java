package org.example.service;

import jakarta.transaction.Transactional;
import org.example.exceptions.MemberNotFoundException;
import org.example.exceptions.OrganizationNotFoundException;
import org.example.exceptions.PermissionDeniedException;
import org.example.exceptions.UserNotFoundException;
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

        // futuro: validação no microserviço de Users
        if (userId == null) {
            logger.warn(MEMBER_ADD, "User not found (userId=null, orgId={})", orgId);
            throw new UserNotFoundException("User not found");
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
