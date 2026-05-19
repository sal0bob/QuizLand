package com.example.test;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private boolean isLoadingProfile = false;

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

        refreshQuizzesList();

    }

    private void showDeleteAccountDialog() {

        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Удаление аккаунта")
                        .setMessage("Вы уверены, что хотите удалить аккаунт?\n\nВсе данные и созданные квизы будут удалены навсегда.")
                        .setPositiveButton("Удалить", null)
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

    private void loadLocalQuizzesWithoutClear() {

        try {
            String json = preferences.getString(KEY_QUIZZES, "[]");

            if (json == null || json.trim().isEmpty()) {
                json = "[]";
            }

            JSONArray quizzesArray = new JSONArray(json);

            LayoutInflater inflater = LayoutInflater.from(this);

            for (int i = 0; i < quizzesArray.length(); i++) {
                JSONObject quizObj = quizzesArray.getJSONObject(i);

                boolean isPublished = quizObj.optBoolean("published", false);

                if (isPublished) {
                    continue;
                }

                final long quizId = quizObj.getLong("id");
                String title = quizObj.getString("title");

                View quizView = inflater.inflate(R.layout.item_quiz, quizzesContainer, false);

                TextView QuizTitle = quizView.findViewById(R.id.quizTitle);
                ImageButton btnDeleteQuiz = quizView.findViewById(R.id.btnDeleteQuiz);
                ImageButton btnPublish = quizView.findViewById(R.id.btnPublish);

                QuizTitle.setText(title);

                QuizTitle.setOnClickListener(v -> {
                    Intent intent = new Intent(ProfileActivity.this, QuizEditorActivity.class);
                    intent.putExtra("quizId", quizId);
                    startActivity(intent);
                });

                btnDeleteQuiz.setOnClickListener(v -> deleteQuiz(quizId));

                btnPublish.setOnClickListener(v -> {
                    Intent intent = new Intent(ProfileActivity.this, PublishQuizActivity.class);
                    intent.putExtra("quizId", quizId);
                    startActivity(intent);
                });

                quizzesContainer.addView(quizView);
            }

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private String createDefaultJpgBase64() {
        Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, 300, 300, paint);

        paint.setColor(Color.BLACK);
        paint.setTextSize(36f);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("QuizLand", 150, 150, paint);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);

        byte[] bytes = outputStream.toByteArray();

        return Base64.encodeToString(bytes, Base64.NO_WRAP);
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
            refreshQuizzesList();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка загрузки: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }


    private void deletePublishedQuiz(long quizId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Удаление квиза")
                .setMessage("Удалить опубликованный квиз с сервера?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    ApiService api = ApiClient.getClient().create(ApiService.class);

                    api.deleteQuizFromServer(quizId).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            Log.d("DELETE_QUIZ", "CODE: " + response.code());

                            try {
                                if (response.isSuccessful()) {
                                    String body = response.body() != null
                                            ? response.body().string()
                                            : "empty body";

                                    Log.d("DELETE_QUIZ", "SUCCESS: " + body);

                                    Toast.makeText(
                                            ProfileActivity.this,
                                            "Квиз удалён с сервера",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    refreshQuizzesList();

                                } else {
                                    String error = response.errorBody() != null
                                            ? response.errorBody().string()
                                            : "empty error";

                                    Log.d("DELETE_QUIZ", "ERROR BODY: " + error);

                                    Toast.makeText(
                                            ProfileActivity.this,
                                            "Ошибка удаления: " + response.code(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            } catch (Exception e) {
                                Log.e("DELETE_QUIZ", "Ошибка чтения ответа", e);
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Log.e("DELETE_QUIZ", "onFailure", t);

                            Toast.makeText(
                                    ProfileActivity.this,
                                    "Ошибка сети: " + t.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showUserQuizzesFromServer(java.util.List<com.example.test.models.QuizResponse> quizzes) {
        LayoutInflater inflater = LayoutInflater.from(this);

        for (com.example.test.models.QuizResponse quiz : quizzes) {
            View quizView = inflater.inflate(R.layout.item_quiz, quizzesContainer, false);

            TextView quizTitle = quizView.findViewById(R.id.quizTitle);
            ImageButton btnDeleteQuiz = quizView.findViewById(R.id.btnDeleteQuiz);
            ImageButton btnPublish = quizView.findViewById(R.id.btnPublish);

            quizTitle.setText(quiz.title != null ? quiz.title : "Квиз без названия");

            btnPublish.setVisibility(View.GONE);

            quizTitle.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, QuizActivity.class);
                intent.putExtra("QUIZ_ID", quiz.id);
                startActivity(intent);
            });

            btnDeleteQuiz.setOnClickListener(v -> {
                if (quiz.id != null) {
                    deletePublishedQuiz(quiz.id);
                } else {
                    Toast.makeText(
                            ProfileActivity.this,
                            "Ошибка: id квиза не найден",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });

            quizzesContainer.addView(quizView);
        }
    }
    private void refreshQuizzesList() {
        if (isLoadingProfile) {
            return;
        }

        isLoadingProfile = true;

        quizzesContainer.removeAllViews();

        loadLocalQuizzesWithoutClear();
        loadProfileFromServer();
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

                isLoadingProfile = false;

                if (response.isSuccessful() && response.body() != null) {

                    com.example.test.models.UserResponse user = response.body();

                    userName.setText(user.username);

                    if (user.quizzes != null) {
                        showUserQuizzesFromServer(user.quizzes);
                    }

                } else {
                    Toast.makeText(ProfileActivity.this,
                            "Ошибка сервера: " + response.code(),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.example.test.models.UserResponse> call, Throwable t) {

                isLoadingProfile = false;

                Toast.makeText(ProfileActivity.this,
                        "Ошибка сети: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }



    @Override
    protected void onResume() {
        super.onResume();

        refreshQuizzesList();

        int countCreate = preferences.getInt("QUIZ_COUNT", 0);
        TextView count = findViewById(R.id.num);
        count.setText(String.valueOf(countCreate));
    }
}
