package com.nationalteammanager;

import org.springframework.boot.SpringApplication;

/** Pokreće aplikaciju nad bazom u memoriji i demo podacima: mvnw spring-boot:test-run uz profile test i ui-demo. */
public class UiDemoApplication {

    public static void main(String[] args) {
        SpringApplication.from(NationalTeamManagerApplication::main).run(args);
    }
}
