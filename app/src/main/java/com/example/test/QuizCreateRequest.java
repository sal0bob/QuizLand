package com.example.test;

import java.util.List;

public class QuizCreateRequest {
    public String title;
    public String description;
    public String preview;
    public Long creator;
    public java.util.List<QuizPageRequest> pages;
}