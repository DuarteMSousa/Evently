package org.evently.users.dtos.User;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Setter
@Getter
public class UserUpdateDTO {

    private UUID id;

    private String username;

    private String email;

    private String password;

    private String nif;

    private Date birthDate;

    private String phoneNumber;

}
