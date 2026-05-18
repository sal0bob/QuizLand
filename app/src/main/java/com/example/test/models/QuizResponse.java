package com.example.test.models;

import java.util.List;

public class QuizResponse {
    public Long id;
    public String title;
    public String description;
    public String previewUrl;
    public Integer rating;
    public Integer visits;
    public String createdAt;
    public List<QuizPageResponse> pages;
}