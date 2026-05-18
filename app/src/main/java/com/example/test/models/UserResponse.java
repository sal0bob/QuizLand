package com.example.test.models;

import java.util.List;

public class UserResponse {
    public long id;
    public String username;
    public String email;
    public int level;
    public String createdAt;

    public List<QuizResponse> quizzes;
}