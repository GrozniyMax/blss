package com.blss.blss;

import org.springframework.boot.SpringApplication;

public class TestBlssApplication {

    public static void main(String[] args) {
        SpringApplication.from(BlssApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
