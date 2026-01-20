package org.example.dtos.organizations;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class OrganizationDTO {

    private UUID id;

    private String name;

    private String description;

    private boolean active;

    private String nipc;

    private String siteUrl;


    private UUID createdBy;
    private Date createdAt;
    private UUID updatedBy;
    private Date updatedAt;

}