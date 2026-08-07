package com.lazar.napo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class NapoApplication {

	public static void main(String[] args) {
		SpringApplication.run(NapoApplication.class, args);
	}

}
