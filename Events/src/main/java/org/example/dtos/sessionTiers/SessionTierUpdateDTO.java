package org.example.dtos.sessionTiers;


import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
@Getter
@Setter
public class SessionTierUpdateDTO {

    private UUID id;

    private float price;
}
