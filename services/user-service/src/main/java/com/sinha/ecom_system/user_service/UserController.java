package com.sinha.ecom_system.user_service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/user-service/users")
public class UserController {

    @Value("${server.port}")
    private String port;

    @GetMapping("")
    public String sayHello() {
        System.out.println("THis instance....");
        return "HELLO WORLD!!!" + " " + port;
    }
}
