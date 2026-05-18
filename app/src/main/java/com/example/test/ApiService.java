package com.example.test;

import com.example.test.models.LoginRequest;
import com.example.test.models.QuizResponse;
import com.example.test.models.SignupRequest;
import com.example.test.models.UserResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @POST("auth/signup")
    Call<UserResponse> signup(@Body SignupRequest request);

    @POST("auth/login")
    Call<UserResponse> login(@Body LoginRequest request);

    @POST("quiz/create")
    Call<String> createQuiz(@Body QuizCreateRequest request);

    @GET("quiz/{id}")
    Call<QuizResponse> getQuiz(@Path("id") long id);

    @GET("user/{id}")
    Call<UserResponse> getUser(@Path("id") long id);

    @DELETE("user/{id}")
    Call<ResponseBody> deleteUser(@Path("id") long id);
}