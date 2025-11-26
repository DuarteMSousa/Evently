package org.example.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("cart-route", r -> r.path("/cart/**").uri("lb://cart"))
                .route("events-route", r -> r.path("/events/**").uri("lb://events"))
                .route("file-generation-route", r -> r.path("/file-generation/**").uri("lb://file-generation"))
                .route("notifications-route", r -> r.path("/notifications/**").uri("lb://notifications"))
                .route("orders-route", r -> r.path("/orders/**").uri("lb://orders"))
                .route("organizations-route", r -> r.path("/organizations/**").uri("lb://organizations"))
                .route("payments-route", r -> r.path("/payments/**").uri("lb://payments"))
                .route("refunds-route", r -> r.path("/refunds/**").uri("lb://refunds"))
                .route("reviews-route", r -> r.path("/reviews/**").uri("lb://reviews"))
                .route("ticket-management-route", r -> r.path("/ticket-management/**").uri("lb://ticket-management"))
                .route("tickets-route", r -> r.path("/tickets/**").uri("lb://tickets"))
                .route("users-route", r -> r.path("/users/**").uri("lb://users"))
                .route("venues-route", r -> r.path("/venues/**").uri("lb://venues"))
                .build();
    }

}
