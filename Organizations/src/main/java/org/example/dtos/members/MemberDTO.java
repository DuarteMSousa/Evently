package org.example.dtos.members;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class MemberDTO {

    private UUID organizationId;
    private UUID userId;

    private UUID createdBy;
    private Date createdAt;
}