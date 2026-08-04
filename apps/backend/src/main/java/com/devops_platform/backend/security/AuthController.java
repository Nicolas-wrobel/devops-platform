package com.devops_platform.backend.security;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.devops_platform.backend.security.dto.LoginRequest;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public AuthController() {
    }

    @PostMapping("login")
    public String login(@RequestBody LoginRequest request) {
        //TODO: process login request
        return "Login successful";
    }
    
}
