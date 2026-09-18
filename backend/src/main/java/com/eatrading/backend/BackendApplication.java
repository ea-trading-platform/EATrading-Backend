package com.eatrading.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		// SpringApplication.run(BackendApplication.class, args);
		Client c = new Client("Erika", "erika.lam@fmr.com");
		System.out.println(c.getName());
	}

}
