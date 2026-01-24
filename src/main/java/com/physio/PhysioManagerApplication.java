package com.physio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PhysioManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhysioManagerApplication.class, args);
    }
}

