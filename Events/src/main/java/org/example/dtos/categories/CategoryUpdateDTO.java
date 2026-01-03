package org.example.dtos.categories;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CategoryUpdateDTO {

    private UUID id;

    private String name;

}
