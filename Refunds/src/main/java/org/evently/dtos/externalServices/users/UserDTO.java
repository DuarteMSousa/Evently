package org.evently.dtos.externalServices.users;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Setter
@Getter
public class UserDTO {

    private UUID id;

    private String username;

    private String email;

    private boolean isActive;

    private String nif;

    private Date birthDate;

    private String phoneNumber;

    private Date createdAt;

    private Date updatedAt;
}