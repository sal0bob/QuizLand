package com.example.test;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PublishQuizActivity extends AppCompatActivity {

    private static final String PREF_NAME = "QUIZ_STORAGE";
    private static final String KEY_QUIZZES = "QUIZZES_LIST";

    private ImageView backImage;
    private TextView backText;
    private TextView quizTitleText;
    private ImageView previewImage;
    private EditText descriptionEditText;
    private Button chooseImageButton;
    private Button publishButton;

    private SharedPreferences quizPreferences;
    private long quizId = -1;
    private String selectedPreviewBase64 = null;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    previewImage.setImageURI(uri);
                    selectedPreviewBase64 = imageUriToJpgBase64(uri);

                    if (selectedPreviewBase64 == null) {
                        Toast.makeText(this, "Не удалось загрузить фото", Toast.LENGTH_LONG).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_publish_quiz);

        quizPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        backImage = findViewById(R.id.backPublishImg);
        backText = findViewById(R.id.backPublishText);
        quizTitleText = findViewById(R.id.publishQuizTitle);
        previewImage = findViewById(R.id.previewImage);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        chooseImageButton = findViewById(R.id.chooseImageButton);
        publishButton = findViewById(R.id.publishButton);

        quizId = getIntent().getLongExtra("quizId", -1);

        if (quizId == -1) {
            Toast.makeText(this, "Ошибка: квиз не найден", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadQuizTitle();

        backImage.setOnClickListener(v -> finish());
        backText.setOnClickListener(v -> finish());

        chooseImageButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        publishButton.setOnClickListener(v -> publishQuizToServer());
    }

    private void loadQuizTitle() {
        try {
            JSONObject quizObj = findLocalQuizById();

            if (quizObj == null) {
                Toast.makeText(this, "Квиз не найден", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            String title = quizObj.optString("title", "Квиз без названия");
            quizTitleText.setText(title);

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка загрузки квиза: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private JSONObject findLocalQuizById() throws Exception {
        String json = quizPreferences.getString(KEY_QUIZZES, "[]");
        JSONArray quizzesArray = new JSONArray(json);

        for (int i = 0; i < quizzesArray.length(); i++) {
            JSONObject quizObj = quizzesArray.getJSONObject(i);

            if (quizObj.getLong("id") == quizId) {
                return quizObj;
            }
        }

        return null;
    }

    private void publishQuizToServer() {
        try {
            JSONObject quizObj = findLocalQuizById();

            if (quizObj == null) {
                Toast.makeText(this, "Квиз не найден!", Toast.LENGTH_SHORT).show();
                return;
            }

            String title = quizObj.optString("title", "Квиз без названия");

            String description = descriptionEditText.getText().toString().trim();
            if (description.isEmpty()) {
                description = "Описание отсутствует";
            }

            JSONArray questionsArray = quizObj.getJSONArray("questions");

            if (questionsArray.length() == 0) {
                Toast.makeText(this, "В квизе нет вопросов", Toast.LENGTH_LONG).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
            long userId = prefs.getLong("USER_ID", -1);

            if (userId == -1) {
                Toast.makeText(this, "Ошибка: userId не найден", Toast.LENGTH_LONG).show();
                return;
            }

            ArrayList<QuizPageRequest> pages = new ArrayList<>();

            for (int i = 0; i < questionsArray.length(); i++) {
                JSONObject questionObj = questionsArray.getJSONObject(i);

                String questionText = questionObj.getString("question");
                JSONArray answersArray = questionObj.getJSONArray("answers");
                int correctIndex = questionObj.getInt("correctIndex");

                if (questionText.trim().isEmpty()) {
                    Toast.makeText(this, "Вопрос №" + (i + 1) + " пустой", Toast.LENGTH_LONG).show();
                    return;
                }

                if (answersArray.length() < 2) {
                    Toast.makeText(this, "У вопроса №" + (i + 1) + " меньше двух ответов", Toast.LENGTH_LONG).show();
                    return;
                }

                if (correctIndex < 0 || correctIndex >= answersArray.length()) {
                    Toast.makeText(this, "У вопроса №" + (i + 1) + " не выбран правильный ответ", Toast.LENGTH_LONG).show();
                    return;
                }

                QuizPageRequest page = new QuizPageRequest();
                page.question = questionText;
                page.options = new LinkedHashMap<>();

                for (int j = 0; j < answersArray.length(); j++) {
                    String answerText = answersArray.getString(j);
                    page.options.put(answerText, j == correctIndex);
                }

                pages.add(page);
            }

            QuizCreateRequest request = new QuizCreateRequest();
            request.title = title;
            request.description = description;
            request.creator = userId;
            request.preview = selectedPreviewBase64 != null
                    ? selectedPreviewBase64
                    : createDefaultJpgBase64();
            request.pages = pages;

            publishButton.setEnabled(false);
            publishButton.setText("Публикация...");

            ApiService apiService = ApiClient.getClient().create(ApiService.class);

            apiService.createQuiz(request).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    Log.d("SERVER", "CODE: " + response.code());

                    publishButton.setEnabled(true);
                    publishButton.setText("Опубликовать");

                    try {
                        if (response.isSuccessful()) {
                            String body = response.body() != null ? response.body().string() : "empty body";
                            Log.d("SERVER", "SUCCESS BODY: " + body);

                            markLocalQuizAsPublished();

                            Toast.makeText(PublishQuizActivity.this, "Квиз опубликован", Toast.LENGTH_LONG).show();

                            Intent intent = new Intent(PublishQuizActivity.this, ProfileActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        } else {
                            String error = response.errorBody() != null ? response.errorBody().string() : "empty error";
                            Log.d("SERVER", "ERROR BODY: " + error);

                            Toast.makeText(PublishQuizActivity.this,
                                    "Ошибка сервера: " + response.code(),
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e("SERVER", "Ошибка чтения ответа", e);
                        Toast.makeText(PublishQuizActivity.this,
                                "Ошибка обработки ответа",
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    publishButton.setEnabled(true);
                    publishButton.setText("Опубликовать");

                    Log.e("SERVER", "onFailure: " + t.getMessage(), t);
                    Toast.makeText(PublishQuizActivity.this,
                            "Ошибка подключения: " + t.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            publishButton.setEnabled(true);
            publishButton.setText("Опубликовать");

            Log.e("PUBLISH", "Ошибка публикации", e);
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void markLocalQuizAsPublished() {
        try {
            String json = quizPreferences.getString(KEY_QUIZZES, "[]");
            JSONArray quizzesArray = new JSONArray(json);

            for (int i = 0; i < quizzesArray.length(); i++) {
                JSONObject quizObj = quizzesArray.getJSONObject(i);

                if (quizObj.getLong("id") == quizId) {
                    quizObj.put("published", true);
                    break;
                }
            }

            quizPreferences.edit()
                    .putString(KEY_QUIZZES, quizzesArray.toString())
                    .apply();

        } catch (Exception e) {
            Log.e("PUBLISH", "Ошибка отметки локального квиза как опубликованного", e);
        }
    }

    private String imageUriToJpgBase64(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);

            if (originalBitmap == null) {
                return null;
            }

            Bitmap scaledBitmap = scaleBitmap(originalBitmap, 800);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);

            byte[] bytes = outputStream.toByteArray();

            return Base64.encodeToString(bytes, Base64.NO_WRAP);

        } catch (Exception e) {
            Log.e("PUBLISH", "Ошибка конвертации картинки", e);
            return null;
        }
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float ratio = Math.min(
                (float) maxSize / width,
                (float) maxSize / height
        );

        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
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
}