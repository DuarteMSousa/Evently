package org.example.services;

import org.example.exceptions.*;
import org.example.models.Member;
import org.example.models.MemberId;
import org.example.models.Organization;
import org.example.repositories.MembersRepository;
import org.example.repositories.OrganizationsRepository;
import org.example.service.OrganizationsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationsServiceTest {

    @Mock private OrganizationsRepository organizationsRepository;
    @Mock private MembersRepository membersRepository;

    @InjectMocks private OrganizationsService organizationsService;

    private Organization validOrg;

    @BeforeEach
    void setup() {
        validOrg = new Organization();
        validOrg.setId(null);
        validOrg.setName("Org A");
        validOrg.setNipc("123456789");
        validOrg.setCreatedBy(UUID.randomUUID());
        validOrg.setActive(true);
    }

    // -----------------------
    // createOrganization
    // -----------------------

    @Test
    void createOrganization_nameNull_throwsInvalidOrganizationException() {
        validOrg.setName(null);

        InvalidOrganizationException ex = assertThrows(InvalidOrganizationException.class,
                () -> organizationsService.createOrganization(validOrg));

        assertEquals("Name is required", ex.getMessage());
        verifyNoInteractions(organizationsRepository, membersRepository);
    }

    @Test
    void createOrganization_nipcNull_throwsInvalidOrganizationException() {
        validOrg.setNipc(null);

        InvalidOrganizationException ex = assertThrows(InvalidOrganizationException.class,
                () -> organizationsService.createOrganization(validOrg));

        assertEquals("NIPC must have exactly 9 digits", ex.getMessage());
        verifyNoInteractions(organizationsRepository, membersRepository);
    }

    @Test
    void createOrganization_nipcInvalid_throwsInvalidOrganizationException() {
        validOrg.setNipc("123");

        InvalidOrganizationException ex = assertThrows(InvalidOrganizationException.class,
                () -> organizationsService.createOrganization(validOrg));

        assertEquals("NIPC must have exactly 9 digits", ex.getMessage());
        verifyNoInteractions(organizationsRepository, membersRepository);
    }

    @Test
    void createOrganization_createdByNull_throwsInvalidOrganizationException() {
        validOrg.setCreatedBy(null);

        InvalidOrganizationException ex = assertThrows(InvalidOrganizationException.class,
                () -> organizationsService.createOrganization(validOrg));

        assertEquals("CreatedBy is required", ex.getMessage());
        verifyNoInteractions(organizationsRepository, membersRepository);
    }

    @Test
    void createOrganization_duplicateNipc_throwsInvalidOrganizationException() {
        when(organizationsRepository.existsByNipc(validOrg.getNipc())).thenReturn(true);

        InvalidOrganizationException ex = assertThrows(InvalidOrganizationException.class,
                () -> organizationsService.createOrganization(validOrg));

        assertEquals("Organization with this NIPC already exists", ex.getMessage());
        verify(organizationsRepository).existsByNipc(validOrg.getNipc());
        verify(organizationsRepository, never()).save(any());
        verifyNoInteractions(membersRepository);
    }

    @Test
    void createOrganization_success_creatorNotMember_createsMember() {
        when(organizationsRepository.existsByNipc(validOrg.getNipc())).thenReturn(false);

        Organization saved = new Organization();
        saved.setId(UUID.randomUUID());
        saved.setName(validOrg.getName());
        saved.setNipc(validOrg.getNipc());
        saved.setCreatedBy(validOrg.getCreatedBy());
        saved.setActive(true);

        when(organizationsRepository.save(any(Organization.class))).thenReturn(saved);
        when(membersRepository.existsById(any(MemberId.class))).thenReturn(false);
        when(membersRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        Organization res = organizationsService.createOrganization(validOrg);

        assertNotNull(res.getId());
        assertTrue(res.isActive());

        verify(membersRepository).save(any(Member.class));
    }

    @Test
    void createOrganization_success_creatorAlreadyMember_doesNotCreateDuplicateMember() {
        when(organizationsRepository.existsByNipc(validOrg.getNipc())).thenReturn(false);

        Organization saved = new Organization();
        saved.setId(UUID.randomUUID());
        saved.setName(validOrg.getName());
        saved.setNipc(validOrg.getNipc());
        saved.setCreatedBy(validOrg.getCreatedBy());
        saved.setActive(true);

        when(organizationsRepository.save(any(Organization.class))).thenReturn(saved);
        when(membersRepository.existsById(any(MemberId.class))).thenReturn(true);

        Organization res = organizationsService.createOrganization(validOrg);

        assertNotNull(res.getId());
        verify(membersRepository, never()).save(any(Member.class));
    }

    // -----------------------
    // updateOrganization
    // -----------------------

    @Test
    void updateOrganization_notFound_throwsOrganizationNotFoundException() {
        UUID orgId = UUID.randomUUID();
        when(organizationsRepository.findById(orgId)).thenReturn(Optional.empty());

        OrganizationNotFoundException ex = assertThrows(OrganizationNotFoundException.class,
                () -> organizationsService.updateOrganization(orgId, new Organization()));

        assertEquals("Organization not found", ex.getMessage());
    }

    @Test
    void updateOrganization_updatedByNull_throwsInvalidOrganizationException() {
        UUID orgId = UUID.randomUUID();

        Organization existing = new Organization();
        existing.setId(orgId);
        existing.setCreatedBy(UUID.randomUUID());
        existing.setNipc("123456789");
        existing.setName("Org");

        when(organizationsRepository.findById(orgId)).thenReturn(Optional.of(existing));

        Organization updates = new Organization();
        updates.setName("Novo");
        updates.setNipc("123456789");
        updates.setUpdatedBy(null);

        InvalidOrganizationException ex = assertThrows(InvalidOrganizationException.class,
                () -> organizationsService.updateOrganization(orgId, updates));

        assertEquals("UpdatedBy is required", ex.getMessage());
        verify(organizationsRepository, never()).save(any());
    }

    @Test
    void updateOrganization_permissionDenied_throwsPermissionDeniedException() {
        UUID orgId = UUID.randomUUID();
        UUID creator = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        Organization existing = new Organization();
        existing.setId(orgId);
        existing.setCreatedBy(creator);
        existing.setNipc("123456789");
        existing.setName("Org");

        when(organizationsRepository.findById(orgId)).thenReturn(Optional.of(existing));

        Organization updates = new Organization();
        updates.setName("Novo");
        updates.setNipc("123456789");
        updates.setUpdatedBy(other);

        PermissionDeniedException ex = assertThrows(PermissionDeniedException.class,
                () -> organizationsService.updateOrganization(orgId, updates));

        assertEquals("User is not the creator of the organization", ex.getMessage());
        verify(organizationsRepository, never()).save(any());
    }

    @Test
    void updateOrganization_newNipcDuplicate_throwsInvalidOrganizationException() {
        UUID orgId = UUID.randomUUID();
        UUID creator = UUID.randomUUID();

        Organization existing = new Organization();
        existing.setId(orgId);
        existing.setCreatedBy(creator);
        existing.setNipc("123456789");
        existing.setName("Org");
        existing.setActive(true);

        when(organizationsRepository.findById(orgId)).thenReturn(Optional.of(existing));
        when(organizationsRepository.existsByNipc("987654321")).thenReturn(true);

        Organization updates = new Organization();
        updates.setName("Org");
        updates.setNipc("987654321");
        updates.setUpdatedBy(creator);
        updates.setActive(true);

        InvalidOrganizationException ex = assertThrows(InvalidOrganizationException.class,
                () -> organizationsService.updateOrganization(orgId, updates));

        assertEquals("Organization with this NIPC already exists", ex.getMessage());
        verify(organizationsRepository, never()).save(any());
    }

    @Test
    void updateOrganization_success_updatesFieldsAndSaves() {
        UUID orgId = UUID.randomUUID();
        UUID creator = UUID.randomUUID();

        Organization existing = new Organization();
        existing.setId(orgId);
        existing.setCreatedBy(creator);
        existing.setNipc("123456789");
        existing.setName("Org");
        existing.setActive(true);

        when(organizationsRepository.findById(orgId)).thenReturn(Optional.of(existing));
        when(organizationsRepository.save(any(Organization.class))).thenAnswer(inv -> inv.getArgument(0));

        Organization updates = new Organization();
        updates.setName("Org Nova");
        updates.setNipc("123456789"); // igual ao existente (não deve validar duplicado)
        updates.setDescription("desc");
        updates.setSiteUrl("https://site");
        updates.setActive(false);
        updates.setUpdatedBy(creator);

        Organization res = organizationsService.updateOrganization(orgId, updates);

        assertEquals("Org Nova", res.getName());
        assertEquals("desc", res.getDescription());
        assertEquals("https://site", res.getSiteUrl());
        assertFalse(res.isActive());
        assertEquals(creator, res.getUpdatedBy());
        verify(organizationsRepository).save(existing);
    }

    // -----------------------
    // getOrganizations / getOrganization / getOrganizationsByUser
    // -----------------------

    @Test
    void getOrganization_notFound_throwsOrganizationNotFoundException() {
        UUID id = UUID.randomUUID();
        when(organizationsRepository.findById(id)).thenReturn(Optional.empty());

        OrganizationNotFoundException ex = assertThrows(OrganizationNotFoundException.class,
                () -> organizationsService.getOrganization(id));

        assertEquals("Organization not found", ex.getMessage());
    }

    @Test
    void getOrganizationsByUser_noMemberships_returnsEmptyList() {
        UUID userId = UUID.randomUUID();
        when(membersRepository.findById_UserId(userId)).thenReturn(Arrays.asList());

        List<Organization> res = organizationsService.getOrganizationsByUser(userId);
        assertTrue(res.isEmpty());
    }

    @Test
    void getOrganizationsByUser_withMemberships_returnsOrganizations() {
        UUID userId = UUID.randomUUID();

        Organization o1 = new Organization(); o1.setId(UUID.randomUUID());
        Organization o2 = new Organization(); o2.setId(UUID.randomUUID());

        Member m1 = new Member(); m1.setOrganization(o1);
        Member m2 = new Member(); m2.setOrganization(o2);

        when(membersRepository.findById_UserId(userId)).thenReturn(Arrays.asList(m1, m2));

        List<Organization> res = organizationsService.getOrganizationsByUser(userId);
        assertEquals(2, res.size());
    }
}