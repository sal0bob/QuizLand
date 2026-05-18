package com.example.test;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.test.models.QuizPageResponse;
import com.example.test.models.QuizResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizActivity extends AppCompatActivity {

    private TextView closeText;
    private ImageView closeImage;
    private TextView timeView;
    private TextView numQuestion;
    private TextView quizCategory;
    private TextView questionText;
    private LinearLayout answersContainer;
    private Button nextButton;

    private QuizResponse quiz;
    private List<QuizPageResponse> pages = new ArrayList<>();

    private int currentQuestionIndex = 0;
    private int correctAnswers = 0;

    private String selectedAnswer = null;
    private LinearLayout selectedAnswerLayout = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz);

        closeText = findViewById(R.id.close);
        closeImage = findViewById(R.id.closeImage);
        timeView = findViewById(R.id.time_view);
        numQuestion = findViewById(R.id.num_question);
        quizCategory = findViewById(R.id.quiz_category);
        questionText = findViewById(R.id.question);
        answersContainer = findViewById(R.id.answers_container);
        nextButton = findViewById(R.id.next_button);

        closeText.setOnClickListener(v -> finish());
        closeImage.setOnClickListener(v -> finish());

        nextButton.setOnClickListener(v -> checkAnswerAndGoNext());

        long quizId = getIntent().getLongExtra("QUIZ_ID", -1);

        if (quizId == -1) {
            Toast.makeText(this, "Ошибка: квиз не найден", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadQuiz(quizId);
    }

    private void loadQuiz(long quizId) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.getQuiz(quizId).enqueue(new Callback<QuizResponse>() {
            @Override
            public void onResponse(Call<QuizResponse> call, Response<QuizResponse> response) {
                Log.d("QUIZ_PLAY", "CODE: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    quiz = response.body();

                    if (quiz.pages == null || quiz.pages.isEmpty()) {
                        Toast.makeText(QuizActivity.this, "В квизе нет вопросов", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    pages = quiz.pages;
                    quizCategory.setText(quiz.title != null ? quiz.title : "Квиз");
                    timeView.setText("00:00");

                    showQuestion();
                } else {
                    Toast.makeText(QuizActivity.this, "Не удалось загрузить квиз", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<QuizResponse> call, Throwable t) {
                Log.e("QUIZ_PLAY", "Ошибка загрузки квиза", t);
                Toast.makeText(QuizActivity.this, "Ошибка сервера: " + t.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void showQuestion() {
        selectedAnswer = null;
        selectedAnswerLayout = null;
        answersContainer.removeAllViews();

        QuizPageResponse page = pages.get(currentQuestionIndex);

        numQuestion.setText((currentQuestionIndex + 1) + "/" + pages.size());
        questionText.setText(page.question != null ? page.question : "Вопрос без текста");

        if (page.options == null || page.options.isEmpty()) {
            Toast.makeText(this, "У вопроса нет вариантов ответа", Toast.LENGTH_LONG).show();
            return;
        }

        int index = 0;

        for (String answer : page.options.keySet()) {
            LinearLayout answerView = createAnswerView(answer, index);
            answersContainer.addView(answerView);
            index++;
        }

        if (currentQuestionIndex == pages.size() - 1) {
            nextButton.setText("Завершить");
        } else {
            nextButton.setText("Далее");
        }
    }

    private LinearLayout createAnswerView(String answerText, int index) {
        LinearLayout answerLayout = new LinearLayout(this);
        answerLayout.setOrientation(LinearLayout.HORIZONTAL);
        answerLayout.setGravity(Gravity.CENTER_VERTICAL);
        answerLayout.setBackgroundResource(R.drawable.quiz_bg);
        answerLayout.setPadding(dp(10), dp(8), dp(10), dp(8));
        answerLayout.setMinimumHeight(dp(55));

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, dp(12));
        answerLayout.setLayoutParams(layoutParams);

        TextView letterView = new TextView(this);
        letterView.setBackgroundResource(R.drawable.letter_bg);
        letterView.setText(getLetter(index));
        letterView.setTextSize(15);
        letterView.setGravity(Gravity.CENTER);
        letterView.setTextColor(Color.parseColor("#5459BD"));

        LinearLayout.LayoutParams letterParams = new LinearLayout.LayoutParams(dp(20), dp(20));
        letterParams.setMargins(0, 0, dp(10), 0);
        letterView.setLayoutParams(letterParams);

        TextView answerTextView = new TextView(this);
        answerTextView.setText(answerText);
        answerTextView.setTextColor(getResources().getColor(R.color.white));
        answerTextView.setTextSize(15);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        answerTextView.setLayoutParams(textParams);

        answerLayout.addView(letterView);
        answerLayout.addView(answerTextView);

        answerLayout.setOnClickListener(v -> selectAnswer(answerLayout, answerText));

        return answerLayout;
    }

    private void selectAnswer(LinearLayout answerLayout, String answerText) {
        if (selectedAnswerLayout != null) {
            selectedAnswerLayout.setBackgroundResource(R.drawable.quiz_bg);
        }

        answerLayout.setBackgroundResource(R.drawable.title_bg);

        selectedAnswer = answerText;
        selectedAnswerLayout = answerLayout;
    }

    private void checkAnswerAndGoNext() {
        if (selectedAnswer == null) {
            Toast.makeText(this, "Выбери ответ", Toast.LENGTH_SHORT).show();
            return;
        }

        QuizPageResponse currentPage = pages.get(currentQuestionIndex);
        Boolean isCorrect = currentPage.options.get(selectedAnswer);

        if (isCorrect != null && isCorrect) {
            correctAnswers++;
        }

        if (currentQuestionIndex < pages.size() - 1) {
            currentQuestionIndex++;
            showQuestion();
        } else {
            showResult();
        }
    }

    private void showResult() {
        Intent intent = new Intent(QuizActivity.this, ResultActivity.class);
        intent.putExtra("CORRECT_ANSWERS", correctAnswers);
        intent.putExtra("TOTAL_QUESTIONS", pages.size());

        if (quiz != null && quiz.title != null) {
            intent.putExtra("QUIZ_TITLE", quiz.title);
        } else {
            intent.putExtra("QUIZ_TITLE", "Квиз завершён");
        }

        startActivity(intent);
        finish();
    }

    private String getLetter(int index) {
        switch (index) {
            case 0:
                return "A";
            case 1:
                return "B";
            case 2:
                return "C";
            case 3:
                return "D";
            case 4:
                return "E";
            case 5:
                return "F";
            default:
                return String.valueOf(index + 1);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}