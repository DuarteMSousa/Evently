package org.evently.users.dtos;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
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
