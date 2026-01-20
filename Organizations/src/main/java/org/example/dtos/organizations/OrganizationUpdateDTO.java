package org.example.dtos.organizations;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class OrganizationUpdateDTO {

    private UUID id;

    private String name;

    private String description;

    private String nipc;

    private String siteUrl;

    private Boolean active;

    private UUID updatedBy;

}