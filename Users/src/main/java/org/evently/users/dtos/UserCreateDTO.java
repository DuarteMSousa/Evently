package org.evently.users.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Setter
@Getter
public class UserCreateDTO {

    private String username;

    private String password;

    private String email;

    private String nif;

    private Date birthDate;

    private String phoneNumber;

}
