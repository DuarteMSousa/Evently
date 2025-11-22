package org.evently.users.dtos.User;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

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
