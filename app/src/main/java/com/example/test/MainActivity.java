package com.example.test;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.test.models.QuizPageListResponse;
import com.example.test.models.QuizResponse;

import android.widget.SearchView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private QuizAdapter quizAdapter;
    private final List<QuizResponse> quizList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean isLogin = prefs.getBoolean("isLogin", false);

        if (!isLogin) {
            Intent intent = new Intent(MainActivity.this, AuthActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        setupQuizRecyclerView();
        loadAllQuizzesFromServer();
        setupSearchView();

        Button buttonGoTest = findViewById(R.id.startGo);

        buttonGoTest.setOnClickListener(v -> {
            if (quizList == null || quizList.isEmpty()) {
                Toast.makeText(
                        MainActivity.this,
                        "Квизы ещё загружаются или список пуст",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Random random = new Random();
            int randomIndex = random.nextInt(quizList.size());

            QuizResponse randomQuiz = quizList.get(randomIndex);

            if (randomQuiz.id == null) {
                Toast.makeText(
                        MainActivity.this,
                        "Ошибка: id квиза не найден",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Intent intent = new Intent(MainActivity.this, QuizActivity.class);
            intent.putExtra("QUIZ_ID", randomQuiz.id);
            startActivity(intent);
        });

        ImageView profilBtn = findViewById(R.id.imageAvatar);
        profilBtn.setOnClickListener(v -> {
            boolean isLoginNow = prefs.getBoolean("isLogin", false);

            if (!isLoginNow) {
                startActivity(new Intent(MainActivity.this, AuthActivity.class));
            } else {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            }
        });
    }

    private void setupQuizRecyclerView() {
        RecyclerView quizRecyclerView = findViewById(R.id.quizRecyclerView);

        quizAdapter = new QuizAdapter(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
        );

        quizRecyclerView.setLayoutManager(layoutManager);
        quizRecyclerView.setAdapter(quizAdapter);
    }

    private void setupSearchView() {
        SearchView searchView = findViewById(R.id.Search);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                quizAdapter.filterByTitle(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                quizAdapter.filterByTitle(newText);
                return true;
            }
        });
    }

    private void loadAllQuizzesFromServer() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.getQuizListSorted("visits,desc").enqueue(new Callback<QuizPageListResponse>() {
            @Override
            public void onResponse(Call<QuizPageListResponse> call, Response<QuizPageListResponse> response) {
                Log.d("QUIZ_LIST", "CODE: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    QuizPageListResponse page = response.body();

                    if (page.content != null) {
                        quizList.clear();
                        quizList.addAll(page.content);

                        Log.d("QUIZ_LIST", "SIZE: " + quizList.size());

                        for (QuizResponse quiz : quizList) {
                            Log.d("QUIZ_LIST", "QUIZ: " + quiz.id + " | " + quiz.title);
                        }

                        quizAdapter.setQuizzes(quizList);
                    } else {
                        Log.d("QUIZ_LIST", "CONTENT IS NULL");
                        Toast.makeText(MainActivity.this, "Список квизов пуст", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Ошибка загрузки: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<QuizPageListResponse> call, Throwable t) {
                Log.e("QUIZ_LIST", "Ошибка загрузки квизов", t);
                Toast.makeText(MainActivity.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean isLogin = prefs.getBoolean("isLogin", false);

        if (!isLogin) {
            Intent intent = new Intent(MainActivity.this, AuthActivity.class);
            startActivity(intent);
            finish();
        } else if (quizAdapter != null) {
            loadAllQuizzesFromServer();
        }
    }
}