package org.example.service;


import org.example.exceptions.MemberNotFoundException;
import org.example.exceptions.OrganizationNotFoundException;
import org.example.exceptions.PermissionDeniedException;
import org.example.exceptions.UserNotFoundException;
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

@Service
public class OrganizationMembersService {

    @Autowired
    private OrganizationsRepository organizationsRepository;

    @Autowired
    private MembersRepository membersRepository;

    private void checkOwnerPermission(Organization org, UUID requesterId) {
        if (!org.getCreatedBy().equals(requesterId)) {
            throw new PermissionDeniedException("User is not the creator of the organization");
        }
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
            // opcional: não permitir adicionar membros a org inativa
            throw new PermissionDeniedException("Cannot add members to an inactive organization");
        }

        // aqui é onde, no futuro, chamarias o microserviço de Users
        if (userId == null) {
            throw new UserNotFoundException("User not found");
        }

        MemberId memberId = new MemberId(orgId, userId);

        if (membersRepository.existsById(memberId)) {
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