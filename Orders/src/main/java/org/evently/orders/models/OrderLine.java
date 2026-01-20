package org.evently.orders.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "order_lines")
@EntityListeners(AuditingEntityListener.class)
public class OrderLine {

    @EmbeddedId
    private OrderLineId id;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, scale = 2)
    private float unitPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("orderId")
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

}
