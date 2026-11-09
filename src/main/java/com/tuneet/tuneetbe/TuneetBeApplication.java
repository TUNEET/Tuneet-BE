package com.tuneet.tuneetbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TuneetBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TuneetBeApplication.class, args);
    }

}
