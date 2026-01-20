package org.example.dtos.organizations;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class OrganizationCreateDTO {

    private String name;

    private String description;

    private String nipc;

    private String siteUrl;

    private UUID createdBy;

}