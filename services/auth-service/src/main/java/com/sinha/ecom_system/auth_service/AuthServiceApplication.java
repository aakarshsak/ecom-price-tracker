package com.sinha.ecom_system.auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.util.TimeZone;

@EnableFeignClients
@SpringBootApplication
@ComponentScan(basePackages = {"com.sinha.ecom_system.common", "com.sinha.ecom_system.auth_service"})
public class AuthServiceApplication {

	static {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
