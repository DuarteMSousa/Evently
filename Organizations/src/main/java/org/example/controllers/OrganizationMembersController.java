package org.example.controllers;

import org.example.dtos.members.MemberDTO;
import org.example.exceptions.MemberNotFoundException;
import org.example.exceptions.OrganizationNotFoundException;
import org.example.exceptions.PermissionDeniedException;
import org.example.exceptions.UserNotFoundException;
import org.example.models.Member;
import org.example.service.OrganizationMembersService;
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
public class OrganizationMembersController {

    @Autowired
    private OrganizationMembersService organizationMembersService;

    private final ModelMapper modelMapper = new ModelMapper();

    private MemberDTO toMemberDTO(Member member) {
        MemberDTO dto = modelMapper.map(member, MemberDTO.class);
        dto.setOrganizationId(member.getId().getOrganizationId());
        dto.setUserId(member.getId().getUserId());
        return dto;
    }

    // GET /get-members/{orgId}
    @GetMapping("/get-members/{orgId}")
    public ResponseEntity<?> getMembers(@PathVariable("orgId") UUID orgId) {
        try {
            List<MemberDTO> members = organizationMembersService.getMembers(orgId)
                    .stream()
                    .map(this::toMemberDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK).body(members);
        } catch (OrganizationNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // POST /add-member/{orgId}/{userId}?requesterId=...
    @PostMapping("/add-member/{orgId}/{userId}")
    public ResponseEntity<?> addMember(@PathVariable("orgId") UUID orgId,
                                       @PathVariable("userId") UUID userId,
                                       @RequestParam("requesterId") UUID requesterId) {
        try {
            Member newMember = organizationMembersService.addMember(orgId, userId, requesterId);
            return ResponseEntity.status(HttpStatus.CREATED).body(toMemberDTO(newMember));
        } catch (PermissionDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (OrganizationNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (UserNotFoundException e) {
            // 405 - Utilizador não encontrado
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // DELETE /remove-member/{orgId}/{userId}?requesterId=...
    @DeleteMapping("/remove-member/{orgId}/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable("orgId") UUID orgId,
                                          @PathVariable("userId") UUID userId,
                                          @RequestParam("requesterId") UUID requesterId) {
        try {
            organizationMembersService.removeMember(orgId, userId, requesterId);
            return ResponseEntity.status(HttpStatus.OK).body("Member removed");
        } catch (PermissionDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (OrganizationNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (MemberNotFoundException e) {
            // 405 - Utilizador não encontrado (aqui podemos dizer que o “user” / membro não existe)
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}