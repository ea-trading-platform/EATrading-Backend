package com.eatrading.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping("/health") 
    public String healthCheck() {
        return "Server online";
    }
}