package org.evently.reviews;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Hello world!
 *
 */
@SpringBootApplication
@EnableFeignClients
public class ReviewsApp
{
    public static void main( String[] args )
    {
        SpringApplication.run(ReviewsApp.class, args);
    }
}
