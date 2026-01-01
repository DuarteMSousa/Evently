package org.evently.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Hello world!
 *
 */
@SpringBootApplication
@EnableFeignClients
public class OrdersApp
{
    public static void main( String[] args )
    {
        SpringApplication.run(OrdersApp.class, args);
    }
}
