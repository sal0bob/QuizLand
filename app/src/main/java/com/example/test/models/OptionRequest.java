package com.example.test.models;

public class OptionRequest {
    public String text;
    public boolean isCorrect;

    public OptionRequest(String text, boolean isCorrect) {
        this.text = text;
        this.isCorrect = isCorrect;
    }
}