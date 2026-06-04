package com.project.flightOps.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AdminController {

    @GetMapping("/users")
    public ResponseEntity<String> getAllUsersTest() {
        return ResponseEntity.ok("Secret Admin Data successfully accessed!");
    }
}
