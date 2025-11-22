package org.evently.users.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Setter
@Getter
public class UserUpdateDTO {

    private String username;

    private String email;

    private String nif;

    private Date birthDate;

    private String phoneNumber;

}
