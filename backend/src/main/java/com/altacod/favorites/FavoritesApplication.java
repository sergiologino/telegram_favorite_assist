package com.altacod.favorites;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FavoritesApplication {

    public static void main(String[] args) {
        SpringApplication.run(FavoritesApplication.class, args);
    }
}
