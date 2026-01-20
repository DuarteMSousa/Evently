package org.evently.users.dtos.User;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserLoginResponseDTO {

    private String token;

    public UserLoginResponseDTO(String token) {
        this.token = token;
    }

}