package com.bank.system.gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Java Configuration for Spring Cloud Gateway routes.
 */
@Configuration
public class GatewayConfig {

    /**
     * Defines the routes for the Gateway.
     *
     * @param builder RouteLocatorBuilder to construct routes.
     * @return Configured RouteLocator.
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Route for User Service
                .route("user_service_route", r -> r
                        .path("/users/**") // Incoming path matching /users/
                        .filters(f -> f.stripPrefix(1)) // Remove /users prefix
                        .uri("lb://" + "USER-SERVICE")) // Forward to User Service via Eureka

                // Route for Payment Service
                .route("payment_service_route", r -> r
                        .path("/payments/**") // Incoming path matching /payments/
                        .filters(f -> f.stripPrefix(1)) // Remove /payments prefix
                        .uri("lb://" + "PAYMENT-SERVICE")) // Forward to Payment Service via Eureka

                // Route for Account Service
                .route("account_service_route", r -> r
                        .path("/accounts/**") // Incoming path matching /accounts/
                        .filters(f -> f.stripPrefix(1)) // Remove /accounts prefix
                        .uri("lb://" + "ACCOUNT-SERVICE")) // Forward to Account Service via Eureka
                .build();
    }
}