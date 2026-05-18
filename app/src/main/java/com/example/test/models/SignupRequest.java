package com.example.test.models;

public class SignupRequest {
    public String username;
    public String email;
    public String password;

    public SignupRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
}