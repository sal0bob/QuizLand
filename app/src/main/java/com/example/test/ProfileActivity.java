package com.example.test;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.test.models.OptionRequest;
import com.google.android.material.button.MaterialButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREF_NAME = "QUIZ_STORAGE";
    private static final String KEY_QUIZZES = "QUIZZES_LIST";

    private LinearLayout quizzesContainer;

    private TextView userName;
    private SharedPreferences preferences;
    private Activity item_quiz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);


        userName = findViewById(R.id.UserName);

        Intent intent = getIntent();

        if (intent.hasExtra("EXTRA_USERNAME")) {
            String username = intent.getStringExtra("EXTRA_USERNAME");

            userName.setText(username);
        }


        ImageButton btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());


        quizzesContainer = findViewById(R.id.quizzesContainer);
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);


        SharedPreferences prefs = getSharedPreferences("QUIZ_STORAGE", MODE_PRIVATE);
        int countCreate = prefs.getInt("QUIZ_COUNT", 0);

        ImageView closeImg = findViewById(R.id.back);
        closeImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        TextView close = findViewById(R.id.close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        ImageView add = findViewById(R.id.add);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, QuizEditorActivity.class);
                startActivity(intent);
            }
        });

        TextView count = findViewById(R.id.num);
        count.setText(String.valueOf(countCreate));

        loadProfileFromServer();


    }

    private void showDeleteAccountDialog() {

        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Удаление аккаунта")
                        .setMessage("Вы уверены, что хотите удалить аккаунт?\n\nВсе данные и созданные квизы будут удалены навсегда.")
                        .setPositiveButton("Удалить", null) // пока null
                        .setNegativeButton("Отмена", (d, which) -> d.dismiss())
                        .setCancelable(false)
                        .create();

        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        new android.os.Handler().postDelayed(() -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        }, 5000);

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            deleteAccount();
            dialog.dismiss();
        });
    }

    private void deleteAccount() {

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        long userId = prefs.getLong("USER_ID", -1);

        if (userId == -1) {
            Toast.makeText(this, "Ошибка: userId не найден", Toast.LENGTH_LONG).show();
            return;
        }

        ApiService api = ApiClient.getClient().create(ApiService.class);

        api.deleteUser(userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (response.isSuccessful()) {

                    SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
                    prefs.edit().clear().commit();

                    SharedPreferences quizPrefs = getSharedPreferences("QUIZ_STORAGE", MODE_PRIVATE);
                    quizPrefs.edit().clear().commit();

                    Toast.makeText(ProfileActivity.this, "Аккаунт удалён", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(ProfileActivity.this, AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(ProfileActivity.this,
                            "Ошибка удаления: " + response.code(),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ProfileActivity.this,
                        "Ошибка сети: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadQuizzes() {
        quizzesContainer.removeAllViews();

        try {
            String json = preferences.getString(KEY_QUIZZES, "[]");

            if (json == null || json.trim().isEmpty()) {
                json = "[]";
            }

            JSONArray quizzesArray = new JSONArray(json);

            LayoutInflater inflater = LayoutInflater.from(this);

            for (int i = 0; i < quizzesArray.length(); i++) {
                JSONObject quizObj = quizzesArray.getJSONObject(i);

                final long quizId = quizObj.getLong("id");
                String title = quizObj.getString("title");

                View quizView = inflater.inflate(R.layout.item_quiz, quizzesContainer, false);

                TextView QuizTitle = quizView.findViewById(R.id.quizTitle);
                ImageButton btnDeleteQuiz = quizView.findViewById(R.id.btnDeleteQuiz);
                ImageButton btnPublish = quizView.findViewById(R.id.btnPublish); // <-- галочка

                QuizTitle.setText(title);

                // открыть квиз
                QuizTitle.setOnClickListener(v -> {
                    Intent intent = new Intent(ProfileActivity.this, QuizEditorActivity.class);
                    intent.putExtra("quizId", quizId);
                    startActivity(intent);
                });

                // удалить квиз
                btnDeleteQuiz.setOnClickListener(v -> deleteQuiz(quizId));

                // опубликовать квиз
                btnPublish.setOnClickListener(v -> {
                    new androidx.appcompat.app.AlertDialog.Builder(ProfileActivity.this)
                            .setTitle("Публикация квиза")
                            .setMessage("Вы уверены, что хотите опубликовать квиз? Он станет доступен всем пользователям.")
                            .setPositiveButton("Опубликовать", (dialog, which) -> {
                                publishQuizToServer(quizId);
                            })
                            .setNegativeButton("Отмена", null)
                            .show();
                });

                quizzesContainer.addView(quizView);
            }

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }





    private void deleteQuiz(long quizId) {
        try {
            String json = preferences.getString(KEY_QUIZZES, "[]");
            JSONArray quizzesArray = new JSONArray(json);

            for (int i = 0; i < quizzesArray.length(); i++) {
                JSONObject quizObj = quizzesArray.getJSONObject(i);

                if (quizObj.getLong("id") == quizId) {
                    quizzesArray.remove(i);
                    break;
                }
            }

            preferences.edit().putString(KEY_QUIZZES, quizzesArray.toString()).apply();

            Toast.makeText(this, "Квиз удалён!", Toast.LENGTH_SHORT).show();
            loadQuizzes();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка загрузки: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void publishQuizToServer(long quizId) {
        try {
            String json = preferences.getString(KEY_QUIZZES, "[]");
            JSONArray quizzesArray = new JSONArray(json);

            JSONObject quizObj = null;

            for (int i = 0; i < quizzesArray.length(); i++) {
                JSONObject temp = quizzesArray.getJSONObject(i);
                if (temp.getLong("id") == quizId) {
                    quizObj = temp;
                    break;
                }
            }

            if (quizObj == null) {
                Toast.makeText(this, "Квиз не найден!", Toast.LENGTH_SHORT).show();
                return;
            }

            String title = quizObj.getString("title");
            JSONArray questionsArray = quizObj.getJSONArray("questions");

            // ⚠️ пока заглушка (потом будет реальный id пользователя)
            long userId = 1;

            java.util.ArrayList<QuizPageRequest> pages = new java.util.ArrayList<>();

            for (int i = 0; i < questionsArray.length(); i++) {
                JSONObject questionObj = questionsArray.getJSONObject(i);

                String questionText = questionObj.getString("question");
                JSONArray answersArray = questionObj.getJSONArray("answers");
                int correctIndex = questionObj.getInt("correctIndex");

                QuizPageRequest page = new QuizPageRequest();
                page.question = questionText;
                page.imageUrl = null;
                page.options = new LinkedHashMap<>();

                for (int j = 0; j < answersArray.length(); j++) {
                    String answerText = answersArray.getString(j);

                    // true если правильный, false если нет
                    page.options.put(answerText, j == correctIndex);
                }

                pages.add(page);
            }

            QuizCreateRequest request = new QuizCreateRequest();
            request.title = title;
            request.description = "Описание отсутствует";
            request.creator = userId;

            // ⚠️ сервер может ожидать preview_url
            request.preview = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+X2Z8AAAAASUVORK5CYII=";
            request.pages = pages;

            ApiService apiService = ApiClient.getClient().create(ApiService.class);

            apiService.createQuiz(request).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {

                    Log.d("SERVER", "CODE: " + response.code());

                    if (response.isSuccessful()) {
                        Log.d("SERVER", "SUCCESS: " + response.body());
                    } else {
                        try {
                            Log.d("SERVER", "ERROR: " + response.errorBody().string());
                        } catch (Exception e) {
                            Log.d("SERVER", "ERROR BODY READ FAILED");
                        }
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.d("SERVER", "FAIL: " + t.getMessage());
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showQuizzesFromServer(java.util.List<com.example.test.models.QuizResponse> quizzes) {


        LayoutInflater inflater = LayoutInflater.from(this);

        for (com.example.test.models.QuizResponse quiz : quizzes) {

            View quizView = inflater.inflate(R.layout.item_quiz, quizzesContainer, false);

            TextView quizTitle = quizView.findViewById(R.id.quizTitle);
            ImageButton btnDeleteQuiz = quizView.findViewById(R.id.btnDeleteQuiz);

            quizTitle.setText(quiz.title);

            quizzesContainer.addView(quizView);
        }
    }

    private void loadProfileFromServer() {

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        long userId = prefs.getLong("USER_ID", -1);

        if (userId == -1) {
            Toast.makeText(this, "Ошибка: userId не найден", Toast.LENGTH_LONG).show();
            return;
        }

        ApiService api = ApiClient.getClient().create(ApiService.class);

        api.getUser(userId).enqueue(new retrofit2.Callback<com.example.test.models.UserResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.example.test.models.UserResponse> call,
                                   retrofit2.Response<com.example.test.models.UserResponse> response) {

                if (response.isSuccessful() && response.body() != null) {

                    com.example.test.models.UserResponse user = response.body();

                    userName.setText(user.username);

                    // квизы пользователя с сервера
                    if (user.quizzes != null) {
                        showQuizzesFromServer(user.quizzes);
                    }

                } else {
                    Toast.makeText(ProfileActivity.this,
                            "Ошибка сервера: " + response.code(),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.example.test.models.UserResponse> call, Throwable t) {
                Toast.makeText(ProfileActivity.this,
                        "Ошибка сети: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }



    @Override
    protected void onResume() {
        super.onResume();
        loadQuizzes();
        loadProfileFromServer();


        int countCreate = preferences.getInt("QUIZ_COUNT", 0);
        TextView count = findViewById(R.id.num);
        count.setText(String.valueOf(countCreate));
    }
}
