package com.example.test;

import com.example.test.models.LoginRequest;
import com.example.test.models.QuizPageListResponse;
import com.example.test.models.QuizResponse;
import com.example.test.models.SignupRequest;
import com.example.test.models.UserResponse;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @POST("auth/signup")
    Call<UserResponse> signup(@Body SignupRequest request);

    @POST("auth/login")
    Call<UserResponse> login(@Body LoginRequest request);

    @POST("quiz/create")
    Call<ResponseBody> createQuiz(@Body QuizCreateRequest request);

    @GET("quiz/{id}")
    Call<QuizResponse> getQuiz(@Path("id") long id);

    @GET("user/{id}")
    Call<UserResponse> getUser(@Path("id") long id);

    @DELETE("user/{id}")
    Call<ResponseBody> deleteUser(@Path("id") long id);

    @GET("quiz/list")
    Call<QuizPageListResponse> getQuizList();

    @GET("quiz/list")
    Call<QuizPageListResponse> getQuizListSorted(@Query("sort") String sort);

    @DELETE("quiz/{id}")
    Call<ResponseBody> deleteQuizFromServer(@Path("id") long id);
}