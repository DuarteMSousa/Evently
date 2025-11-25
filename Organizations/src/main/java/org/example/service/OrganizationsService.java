package org.example.service;

import org.example.exceptions.*;
import org.example.models.Member;
import org.example.models.MemberId;
import org.example.models.Organization;
import org.example.repositories.MembersRepository;
import org.example.repositories.OrganizationsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class OrganizationsService {

    @Autowired
    private OrganizationsRepository organizationsRepository;

    @Autowired
    private MembersRepository membersRepository;

    private static final Pattern NIPC_PATTERN = Pattern.compile("\\d{9}");


    private void validateOrganization(Organization org, boolean isCreate) {
        if (org.getName() == null) {
            throw new InvalidOrganizationException("Name is required");
        }
        if (org.getNipc() == null || !NIPC_PATTERN.matcher(org.getNipc()).matches()) {
            throw new InvalidOrganizationException("NIPC must have exactly 9 digits");
        }
        if (isCreate && org.getCreatedBy() == null) {
            throw new InvalidOrganizationException("CreatedBy is required");
        }
        if (!isCreate && org.getUpdatedBy() == null) {
            throw new InvalidOrganizationException("UpdatedBy is required");
        }
    }

    private void checkOwnerPermission(Organization org, UUID requesterId) {
        if (!org.getCreatedBy().equals(requesterId)) {
            throw new PermissionDeniedException("User is not the creator of the organization");
        }
    }


    @Transactional
    public Organization createOrganization(Organization org) {
        validateOrganization(org, true);

        if (organizationsRepository.existsByNipc(org.getNipc())) {
            throw new InvalidOrganizationException("Organization with this NIPC already exists");
        }

        org.setActive(true);
        Organization saved = organizationsRepository.save(org);

        UUID orgId = saved.getId();
        UUID creatorId = saved.getCreatedBy();

        if (creatorId == null) {
            throw new InvalidOrganizationException("CreatedBy is required");
        }

        MemberId memberId = new MemberId(orgId, creatorId);

        if (!membersRepository.existsById(memberId)) {
            Member member = new Member();
            member.setId(memberId);
            member.setOrganization(saved);
            member.setCreatedBy(creatorId);

            membersRepository.save(member);
        }
        return saved;
    }


    @Transactional
    public Organization updateOrganization(UUID orgId, Organization orgWithUpdates) {
        Organization existing = organizationsRepository.findById(orgId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));

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
        if (orgWithUpdates.getNipc() != null &&
                !orgWithUpdates.getNipc().equals(existing.getNipc())) {

            if (!NIPC_PATTERN.matcher(orgWithUpdates.getNipc()).matches()) {
                throw new InvalidOrganizationException("NIPC must have exactly 9 digits");
            }

            if (organizationsRepository.existsByNipc(orgWithUpdates.getNipc())) {
                throw new InvalidOrganizationException("Organization with this NIPC already exists");
            }

            existing.setNipc(orgWithUpdates.getNipc());
        }
        if (orgWithUpdates.isActive() != existing.isActive()) {
            existing.setActive(orgWithUpdates.isActive());
        }

        existing.setUpdatedBy(orgWithUpdates.getUpdatedBy());

        return organizationsRepository.save(existing);
    }

    public List<Organization> getOrganizations() {
        return organizationsRepository.findAll();
    }

    public Organization getOrganization(UUID orgId) {
        return organizationsRepository.findById(orgId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));
    }


    public List<Member> getMembers(UUID orgId) {
        if (!organizationsRepository.existsById(orgId)) {
            throw new OrganizationNotFoundException("Organization not found");
        }
        return membersRepository.findByOrganization_Id(orgId);
    }

    @Transactional
    public Member addMember(UUID orgId, UUID userId, UUID requesterId) {

        Organization org = organizationsRepository.findById(orgId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));

        checkOwnerPermission(org, requesterId);

        if (!org.isActive()) {
            throw new InvalidOrganizationException("Cannot add members to an inactive organization");
        }

        // Aqui seria o local para chamar o microserviço de Users para verificar se o user existe
        // Por enquanto, assumimos que o user existe.
        if (userId == null) {
            throw new UserNotFoundException("User not found");
        }

        MemberId memberId = new MemberId(orgId, userId);

        if (membersRepository.existsById(memberId)) {
            // já é membro, podemos simplesmente retornar ou lançar exceção.
            // Vou apenas retornar o existente.
            return membersRepository.findById(memberId).get();
        }

        Member member = new Member();
        member.setId(memberId);
        member.setOrganization(org);
        member.setCreatedBy(requesterId);

        return membersRepository.save(member);
    }

    @Transactional
    public void removeMember(UUID orgId, UUID userId, UUID requesterId) {

        Organization org = organizationsRepository.findById(orgId)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));

        checkOwnerPermission(org, requesterId);

        MemberId memberId = new MemberId(orgId, userId);

        Member member = membersRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Member not found"));

        membersRepository.delete(member);
    }
}