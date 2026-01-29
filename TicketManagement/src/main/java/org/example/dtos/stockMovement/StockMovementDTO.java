package org.example.dtos.stockMovement;

import lombok.Getter;
import lombok.Setter;
import org.example.enums.StockMovementType;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class StockMovementDTO {

    private UUID id;

    private Integer quantity;

    private StockMovementType type;

    private Date createdAt;

}
