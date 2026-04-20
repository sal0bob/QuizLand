package com.example.test;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class ProfileActivity extends AppCompatActivity {

    private static final String PREF_NAME = "QUIZ_STORAGE";
    private static final String KEY_QUIZZES = "QUIZZES_LIST";

    private LinearLayout quizzesContainer;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);



        quizzesContainer = findViewById(R.id.quizzesContainer);
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        loadQuizzes();

        SharedPreferences prefs = getSharedPreferences("QUIZ_STORAGE", MODE_PRIVATE);
        int countCreate = prefs.getInt("QUIZ_COUNT", 0);

        LinearLayout close = findViewById(R.id.back_view);
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

                long quizId = quizObj.getLong("id");
                String title = quizObj.getString("title");

                View quizView = inflater.inflate(R.layout.item_quiz, quizzesContainer, false);

                TextView QuizTitle = quizView.findViewById(R.id.quizTitle);
                ImageButton btnDeleteQuiz = quizView.findViewById(R.id.btnDeleteQuiz);
                QuizTitle.setOnClickListener(v -> {
                    Intent intent = new Intent(ProfileActivity.this, QuizEditorActivity.class);
                    intent.putExtra("quizId", quizId);
                    startActivity(intent);
                });

                QuizTitle.setText(title);

                btnDeleteQuiz.setOnClickListener(v -> deleteQuiz(quizId));

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
            loadQuizzes(); // обновляем список на экране

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка загрузки: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadQuizzes();

        // обновляем счётчик
        int countCreate = preferences.getInt("QUIZ_COUNT", 0);
        TextView count = findViewById(R.id.num);
        count.setText(String.valueOf(countCreate));
    }
}
