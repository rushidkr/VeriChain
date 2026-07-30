package com.verichain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VerichainApplication {

    public static void main(String[] args) {
        SpringApplication.run(VerichainApplication.class, args);
    }
}
