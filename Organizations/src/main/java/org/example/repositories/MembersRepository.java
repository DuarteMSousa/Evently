package org.example.repositories;

import org.example.models.Member;
import org.example.models.MemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MembersRepository extends JpaRepository<Member, MemberId> {

    List<Member> findByOrganization_Id(UUID organizationId);

    List<Member> findById_UserId(UUID userId);
}