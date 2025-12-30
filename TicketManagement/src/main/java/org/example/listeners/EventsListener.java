package org.example.listeners;

import org.example.config.MQConfig;
import org.example.models.TicketStock;
import org.example.models.TicketStockId;
import org.example.services.TicketStocksService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventsListener {

    @Autowired
    private TicketStocksService ticketStocksService;

    @RabbitListener(queues = MQConfig.EVENT_QUEUE)
    public void listener(EventPublishedEvent event) {
        for (EventSessionDTO session : event.sessions) {
            for (SessionTierDTO tier : session.tiers) {
                TicketStock stock = new TicketStock();
                stock.setAvailableQuantity();
                TicketStockId stockId = new TicketStockId();
                stockId.setEventId(event.getId());
                stockId.setSessionId(session.getId());
                stockId.setTierId(tier.getId());
                stock.setId(stockId);

                ticketStocksService.createTicketStock(stock);
            }
        }
    }
}
