package org.example.service;

import jakarta.transaction.Transactional;
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
import java.util.regex.Pattern;

@Service
public class OrganizationsService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationsService.class);

    private static final Marker ORG_VALIDATE = MarkerFactory.getMarker("ORG_VALIDATE");
    private static final Marker ORG_PERMISSION = MarkerFactory.getMarker("ORG_PERMISSION");
    private static final Marker ORG_CREATE = MarkerFactory.getMarker("ORG_CREATE");
    private static final Marker ORG_UPDATE = MarkerFactory.getMarker("ORG_UPDATE");
    private static final Marker ORG_GET = MarkerFactory.getMarker("ORG_GET");
    private static final Marker ORG_LIST = MarkerFactory.getMarker("ORG_LIST");
    private static final Marker ORG_MEMBERS_GET = MarkerFactory.getMarker("ORG_MEMBERS_GET");
    private static final Marker ORG_MEMBER_ADD = MarkerFactory.getMarker("ORG_MEMBER_ADD");
    private static final Marker ORG_MEMBER_REMOVE = MarkerFactory.getMarker("ORG_MEMBER_REMOVE");

    @Autowired
    private OrganizationsRepository organizationsRepository;

    @Autowired
    private MembersRepository membersRepository;

    private static final Pattern NIPC_PATTERN = Pattern.compile("\\d{9}");

    /**
     * Validates the organization payload depending on the operation type.
     *
     * Validation rules:
     * - name is mandatory
     * - nipc must have exactly 9 digits
     * - on create: createdBy is mandatory
     * - on update: updatedBy is mandatory
     *
     * @param org organization payload to validate
     * @param isCreate true for create operations, false for update operations
     * @throws InvalidOrganizationException if any required field is missing or invalid
     */
    private void validateOrganization(Organization org, boolean isCreate) {
        logger.debug(ORG_VALIDATE,
                "Validating organization (isCreate={}, orgId={}, name={}, nipcPresent={}, createdByPresent={}, updatedByPresent={})",
                isCreate,
                org != null ? org.getId() : null,
                org != null ? org.getName() : null,
                org != null && org.getNipc() != null,
                org != null && org.getCreatedBy() != null,
                org != null && org.getUpdatedBy() != null
        );

        if (org.getName() == null) {
            logger.warn(ORG_VALIDATE, "Missing name");
            throw new InvalidOrganizationException("Name is required");
        }
        if (org.getNipc() == null || !NIPC_PATTERN.matcher(org.getNipc()).matches()) {
            logger.warn(ORG_VALIDATE, "Invalid NIPC format (nipc={})", org.getNipc());
            throw new InvalidOrganizationException("NIPC must have exactly 9 digits");
        }
        if (isCreate && org.getCreatedBy() == null) {
            logger.warn(ORG_VALIDATE, "Missing createdBy on create");
            throw new InvalidOrganizationException("CreatedBy is required");
        }
        if (!isCreate && org.getUpdatedBy() == null) {
            logger.warn(ORG_VALIDATE, "Missing updatedBy on update");
            throw new InvalidOrganizationException("UpdatedBy is required");
        }
    }

    /**
     * Checks if the requester has permissions to manage the organization.
     *
     * Current rule:
     * - requester must be the organization creator (org.createdBy == requesterId).
     *
     * @param org organization instance
     * @param requesterId requester identifier
     * @throws PermissionDeniedException if requester is not the organization creator or IDs are missing
     */
    private void checkOwnerPermission(Organization org, UUID requesterId) {
        if (org.getCreatedBy() == null || requesterId == null) {
            logger.warn(ORG_PERMISSION, "Permission check failed due to missing ids (orgCreatedBy={}, requesterId={})",
                    org.getCreatedBy(), requesterId);
            throw new PermissionDeniedException("User is not the creator of the organization");
        }

        if (!org.getCreatedBy().equals(requesterId)) {
            logger.warn(ORG_PERMISSION, "Permission denied (orgId={}, orgCreatedBy={}, requesterId={})",
                    org.getId(), org.getCreatedBy(), requesterId);
            throw new PermissionDeniedException("User is not the creator of the organization");
        }

        logger.debug(ORG_PERMISSION, "Permission granted (orgId={}, requesterId={})", org.getId(), requesterId);
    }

    /**
     * Creates a new organization after validating its payload.
     *
     * Additional behavior:
     * - nipc must be unique
     * - organization is created as active
     * - creator is automatically added as a member of the organization (idempotent if already member)
     *
     * @param org organization to create
     * @return persisted organization
     * @throws InvalidOrganizationException if payload is invalid, nipc already exists, or createdBy is missing
     */
    @Transactional
    public Organization createOrganization(Organization org) {
        logger.info(ORG_CREATE, "Create organization requested (name={}, nipc={}, createdBy={})",
                org != null ? org.getName() : null,
                org != null ? org.getNipc() : null,
                org != null ? org.getCreatedBy() : null
        );

        validateOrganization(org, true);

        if (organizationsRepository.existsByNipc(org.getNipc())) {
            logger.warn(ORG_CREATE, "Organization already exists with NIPC {}", org.getNipc());
            throw new InvalidOrganizationException("Organization with this NIPC already exists");
        }

        org.setActive(true);
        Organization saved = organizationsRepository.save(org);

        logger.info(ORG_CREATE, "Organization created (orgId={}, nipc={}, active={})",
                saved.getId(), saved.getNipc(), saved.isActive());

        UUID orgId = saved.getId();
        UUID creatorId = saved.getCreatedBy();

        if (creatorId == null) {
            logger.warn(ORG_CREATE, "CreatedBy missing after save (orgId={})", orgId);
            throw new InvalidOrganizationException("CreatedBy is required");
        }

        MemberId memberId = new MemberId(orgId, creatorId);

        if (!membersRepository.existsById(memberId)) {
            Member member = new Member();
            member.setId(memberId);
            member.setOrganization(saved);
            member.setCreatedBy(creatorId);

            membersRepository.save(member);

            logger.info(ORG_CREATE, "Creator added as member (orgId={}, userId={})", orgId, creatorId);
        } else {
            logger.debug(ORG_CREATE, "Creator already member (orgId={}, userId={})", orgId, creatorId);
        }

        return saved;
    }

    /**
     * Updates an existing organization.
     *
     * Update rules:
     * - organization must exist
     * - updatedBy is mandatory (validated in validateOrganization)
     * - requester must be the organization creator (permission check uses updatedBy as requesterId)
     * - nipc change is allowed only if it is valid (9 digits) and unique
     * - fields are updated conditionally (only non-null fields are applied)
     *
     * @param orgId organization identifier
     * @param orgWithUpdates organization payload with updates (must include updatedBy)
     * @return updated organization
     * @throws OrganizationNotFoundException if the organization does not exist
     * @throws InvalidOrganizationException if update payload is invalid or nipc change is invalid/duplicate
     * @throws PermissionDeniedException if requester is not the creator of the organization
     */
    @Transactional
    public Organization updateOrganization(UUID orgId, Organization orgWithUpdates) {
        logger.info(ORG_UPDATE, "Update organization requested (orgId={}, updatedBy={})",
                orgId, orgWithUpdates != null ? orgWithUpdates.getUpdatedBy() : null);

        Organization existing = organizationsRepository.findById(orgId)
                .orElseThrow(() -> {
                    logger.warn(ORG_UPDATE, "Organization not found (orgId={})", orgId);
                    return new OrganizationNotFoundException("Organization not found");
                });

        validateOrganization(orgWithUpdates, false);

        checkOwnerPermission(existing, orgWithUpdates.getUpdatedBy());

        if (orgWithUpdates.getName() != null) {
            existing.setName(orgWithUpdates.getName());
        }
        if (orgWithUpdates.getDescription() != null) {
            existing.setDescription(orgWithUpdates.getDescription());
        }
        if (orgWithUpdates.getSiteUrl() != null) {
            existing.setSiteUrl(orgWithUpdates.getSiteUrl());
        }

        if (orgWithUpdates.getNipc() != null && !orgWithUpdates.getNipc().equals(existing.getNipc())) {
            if (!NIPC_PATTERN.matcher(orgWithUpdates.getNipc()).matches()) {
                logger.warn(ORG_UPDATE, "Invalid new NIPC format (orgId={}, nipc={})", orgId, orgWithUpdates.getNipc());
                throw new InvalidOrganizationException("NIPC must have exactly 9 digits");
            }

            if (organizationsRepository.existsByNipc(orgWithUpdates.getNipc())) {
                logger.warn(ORG_UPDATE, "New NIPC already exists (orgId={}, nipc={})", orgId, orgWithUpdates.getNipc());
                throw new InvalidOrganizationException("Organization with this NIPC already exists");
            }

            existing.setNipc(orgWithUpdates.getNipc());
            logger.info(ORG_UPDATE, "NIPC updated (orgId={}, nipc={})", orgId, existing.getNipc());
        }

        if (orgWithUpdates.isActive() != existing.isActive()) {
            existing.setActive(orgWithUpdates.isActive());
            logger.info(ORG_UPDATE, "Active flag changed (orgId={}, active={})", orgId, existing.isActive());
        }

        existing.setUpdatedBy(orgWithUpdates.getUpdatedBy());

        Organization saved = organizationsRepository.save(existing);

        logger.info(ORG_UPDATE, "Organization updated successfully (orgId={})", saved.getId());

        return saved;
    }

    /**
     * Retrieves all organizations.
     *
     * @return list of organizations
     */
    public List<Organization> getOrganizations() {
        logger.debug(ORG_LIST, "Get organizations requested");
        List<Organization> orgs = organizationsRepository.findAll();
        logger.debug(ORG_LIST, "Get organizations completed (results={})", orgs.size());
        return orgs;
    }

    /**
     * Retrieves an organization by its unique identifier.
     *
     * @param orgId organization identifier
     * @return found organization
     * @throws OrganizationNotFoundException if the organization does not exist
     */
    public Organization getOrganization(UUID orgId) {
        logger.debug(ORG_GET, "Get organization requested (orgId={})", orgId);

        return organizationsRepository.findById(orgId)
                .orElseThrow(() -> {
                    logger.warn(ORG_GET, "Organization not found (orgId={})", orgId);
                    return new OrganizationNotFoundException("Organization not found");
                });
    }
}
