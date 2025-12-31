package org.example.services;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.example.clients.UsersClient;
import org.example.exceptions.*;
import org.example.models.Member;
import org.example.models.MemberId;
import org.example.models.Organization;
import org.example.repositories.MembersRepository;
import org.example.repositories.OrganizationsRepository;
import org.example.service.OrganizationMembersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationMembersServiceTest {

    @Mock private OrganizationsRepository organizationsRepository;
    @Mock private MembersRepository membersRepository;
    @Mock private UsersClient usersClient;

    @InjectMocks private OrganizationMembersService organizationMembersService;

    private UUID orgId;
    private UUID creatorId;
    private UUID userId;
    private Organization org;

    @BeforeEach
    void setup() {
        orgId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        userId = UUID.randomUUID();

        org = new Organization();
        org.setId(orgId);
        org.setCreatedBy(creatorId);
        org.setActive(true);
    }

    // helper: Feign 404
    private FeignException notFoundFeign() {
        Request req = Request.create(Request.HttpMethod.GET, "/users/" + userId, Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        Response resp = Response.builder().status(404).request(req).headers(Collections.emptyMap()).build();
        return new FeignException.NotFound("not found", req, null, resp.headers());
    }

    // helper: Feign generic (ex 503)
    private FeignException genericFeign(int status) {
        Request req = Request.create(Request.HttpMethod.GET, "/users/" + userId, Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        Response resp = Response.builder().status(status).request(req).headers(Collections.emptyMap()).build();
        return FeignException.errorStatus("UsersClient#getUser", resp);
    }

    // -----------------------
    // getMembers
    // -----------------------

    @Test
    void getMembers_orgNotExists_throwsOrganizationNotFoundException() {
        when(organizationsRepository.existsById(orgId)).thenReturn(false);

        OrganizationNotFoundException ex = assertThrows(OrganizationNotFoundException.class,
                () -> organizationMembersService.getMembers(orgId));

        assertEquals("Organization not found", ex.getMessage());
        verify(membersRepository, never()).findByOrganization_Id(any());
    }

    @Test
    void getMembers_success_returnsList() {
        when(organizationsRepository.existsById(orgId)).thenReturn(true);
        when(membersRepository.findByOrganization_Id(orgId)).thenReturn(Arrays.asList(new Member(), new Member()));

        List<Member> res = organizationMembersService.getMembers(orgId);

        assertEquals(2, res.size());
    }

    // -----------------------
    // addMember
    // -----------------------

    @Test
    void addMember_orgNotFound_throwsOrganizationNotFoundException() {
        when(organizationsRepository.findById(orgId)).thenReturn(Optional.empty());

        OrganizationNotFoundException ex = assertThrows(OrganizationNotFoundException.class,
                () -> organizationMembersService.addMember(orgId, userId, creatorId));

        assertEquals("Organization not found", ex.getMessage());
    }

    @Test
    void addMember_permissionDenied_requesterNotOwner_throwsPermissionDeniedException() {
        when(organizationsRepository.findById(orgId)).thenReturn(Optional.of(org));

        UUID other = UUID.randomUUID();

        PermissionDeniedException ex = assertThrows(PermissionDeniedException.class,
                () -> organizationMembersService.addMember(orgId, userId, other));

        assertEquals("User is not the creator of the organization", ex.getMessage());
        verifyNoInteractions(usersClient);
        verify(membersRepository, never()).save(any());
    }

    @Test
    void addMember_orgInactive_throwsPermissionDeniedException() {
        org.setActive(false);
        when(organizationsRepository.findById(orgId)).thenReturn(Optional.of(org));

        PermissionDeniedException ex = assertThrows(PermissionDeniedException.class,
                () -> organizationMembersService.addMember(orgId, userId, creatorId));

        assertEquals("Cannot add members to an inactive organization", ex.getMessage());
        verifyNoInteractions(usersClient);
        verify(membersRepository, never()).save(any());
    }

    @Test
    void addMember_userIdNull_throwsUserNotFoundException() {
        when(organizationsRepository.findById(orgId)).thenReturn(Optional.of(org));

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> organizationMembersService.addMember(orgId, null, creatorId));

        assertEquals("User not found", ex.getMessage());
        verifyNoInteractions(usersClient);
        verify(membersRepository, never()).save(any());
    }

    @Test
    void addMember_usersService404_throwsUserNotFoundException() {
        when(organizationsRepository.findById(orgId)).thenReturn(Optional.of(org));
        doThrow(notFoundFeign()).when(usersClient).getUser(userId);

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> organizationMembersService.addMember(orgId, userId, creatorId));

        assertEquals("User not found", ex.getMessage());
        verify(membersRepository, never()).save(any());
    }

    @Test
    void addMember_usersServiceError_throwsExternalServiceException() {
        when(organizationsRepository.findById(orgId)).thenReturn(Optional.of(org));
        doThrow(genericFeign(503)).when(usersClient).getUser(userId);

        ExternalServiceException ex = assertThrows(ExternalServiceException.class,
                () -> organizationMembersService.addMember(orgId, userId, creatorId));

        assertEquals("Users service unavailable", ex.getMessage());
        verify(membersRepository, never()).save(any());
    }

    @Test
    void addMember_alreadyMember_returnsExisting() {
        when(organizationsRepository.findById(orgId)).thenReturn(Optional.of(org));

        when(usersClient.getUser(userId)).thenReturn(null);

        MemberId mid = new MemberId(orgId, userId);
        Member existing = new Member();
        existing.setId(mid);

        when(membersRepository.existsById(mid)).thenReturn(true);
        when(membersRepository.findById(mid)).thenReturn(Optional.of(existing));

        Member res = organizationMembersService.addMember(orgId, userId, creatorId);

        assertEquals(mid, res.getId());
        verify(membersRepository, never()).save(any());
    }


    @Test
    void addMember_success_savesMember() {
        when(organizationsRepository.findById(orgId)).thenReturn(Optional.of(org));

        MemberId mid = new MemberId(orgId, userId);
        when(membersRepository.existsById(mid)).thenReturn(false);

        when(membersRepository.save(any(Member.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(usersClient.getUser(userId)).thenReturn(null);

        Member res = organizationMembersService.addMember(orgId, userId, creatorId);

        assertNotNull(res.getId());
        assertEquals(orgId, res.getId().getOrganizationId());
        assertEquals(userId, res.getId().getUserId());
        assertEquals(creatorId, res.getCreatedBy());

        verify(membersRepository).save(any(Member.class));
    }

    // -----------------------
    // removeMember
    // -----------------------

    @Test
    void removeMember_orgNotFound_throwsOrganizationNotFoundException() {
        when(organizationsRepository.findById(orgId)).thenReturn(Optional.empty());

        OrganizationNotFoundException ex = assertThrows(OrganizationNotFoundException.class,
                () -> organizationMembersService.removeMember(orgId, userId, creatorId));

        assertEquals("Organization not found", ex.getMessage());
    }

    @Test
    void removeMember_permissionDenied_throwsPermissionDeniedException() {
        when(organizationsRepository.findById(orgId)).thenReturn(Optional.of(org));
        UUID other = UUID.randomUUID();

        PermissionDeniedException ex = assertThrows(PermissionDeniedException.class,
                () -> organizationMembersService.removeMember(orgId, userId, other));

        assertEquals("User is not the creator of the organization", ex.getMessage());
        verify(membersRepository, never()).delete(any());
    }

    @Test
    void removeMember_memberNotFound_throwsMemberNotFoundException() {
        when(organizationsRepository.findById(orgId)).thenReturn(Optional.of(org));
        MemberId mid = new MemberId(orgId, userId);

        when(membersRepository.findById(mid)).thenReturn(Optional.empty());

        MemberNotFoundException ex = assertThrows(MemberNotFoundException.class,
                () -> organizationMembersService.removeMember(orgId, userId, creatorId));

        assertEquals("Member not found", ex.getMessage());
        verify(membersRepository, never()).delete(any());
    }

    @Test
    void removeMember_success_deletesMember() {
        when(organizationsRepository.findById(orgId)).thenReturn(Optional.of(org));
        MemberId mid = new MemberId(orgId, userId);

        Member member = new Member();
        member.setId(mid);

        when(membersRepository.findById(mid)).thenReturn(Optional.of(member));

        organizationMembersService.removeMember(orgId, userId, creatorId);

        verify(membersRepository).delete(member);
    }
}
