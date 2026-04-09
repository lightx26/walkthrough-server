package com.pet.walkthroughserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WalkthroughServerApplication {

    public static void main(String[] args) {
        System.out.println("Starting Walkthrough Server Application...");
        SpringApplication.run(WalkthroughServerApplication.class, args);
    }

}
