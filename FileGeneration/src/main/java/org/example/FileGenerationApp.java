package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Hello world!
 *
 */

@SpringBootApplication
@EnableFeignClients
public class FileGenerationApp
{
    public static void main( String[] args )
    {
        SpringApplication.run(FileGenerationApp.class, args);
    }
}
