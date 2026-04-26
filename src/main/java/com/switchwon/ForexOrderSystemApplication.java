package com.switchwon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ForexOrderSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ForexOrderSystemApplication.class, args);
    }
}