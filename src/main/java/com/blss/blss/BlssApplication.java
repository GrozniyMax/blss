package com.blss.blss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@EnableJdbcRepositories
public class BlssApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlssApplication.class, args);
    }

}
