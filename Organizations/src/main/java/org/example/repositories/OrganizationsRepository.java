package org.example.repositories;

import org.example.models.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganizationsRepository extends JpaRepository<Organization, UUID> {

    boolean existsByNipc(String nipc);

}