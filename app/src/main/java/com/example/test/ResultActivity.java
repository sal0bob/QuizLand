package com.example.test;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {

    private TextView resultTitle;
    private TextView resultScore;
    private TextView resultPercent;
    private TextView resultMessage;
    private TextView resultXp;
    private TextView resultLevel;

    private Button backToMainButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_result);

        resultTitle = findViewById(R.id.resultTitle);
        resultScore = findViewById(R.id.resultScore);
        resultPercent = findViewById(R.id.resultPercent);
        resultMessage = findViewById(R.id.resultMessage);
        resultXp = findViewById(R.id.resultXp);
        resultLevel = findViewById(R.id.resultLevel);
        backToMainButton = findViewById(R.id.backToMainButton);

        int correctAnswers = getIntent().getIntExtra("CORRECT_ANSWERS", 0);
        int totalQuestions = getIntent().getIntExtra("TOTAL_QUESTIONS", 0);
        String quizTitle = getIntent().getStringExtra("QUIZ_TITLE");

        if (quizTitle == null || quizTitle.trim().isEmpty()) {
            quizTitle = "Квиз завершён";
        }

        resultTitle.setText(quizTitle);
        resultScore.setText(correctAnswers + " / " + totalQuestions);

        int percent = 0;

        if (totalQuestions > 0) {
            percent = Math.round((correctAnswers * 100f) / totalQuestions);
        }

        resultPercent.setText(percent + "%");

        if (percent == 100) {
            StatsManager.incrementPerfectWins(this);
        }

        if (percent >= 90) {
            resultMessage.setText("Отличный результат! Ты почти эксперт!");
        } else if (percent >= 70) {
            resultMessage.setText("Хорошо! Ещё немного практики — и будет идеально.");
        } else if (percent >= 50) {
            resultMessage.setText("Неплохо! Но есть куда расти.");
        } else {
            resultMessage.setText("Не переживай, попробуй пройти ещё раз.");
        }

        int playersWithPerfectScore = 0;
        int totalPlayers = 0;

        int earnedXp = LevelManager.calculateEarnedXp(
                correctAnswers,
                totalQuestions,
                playersWithPerfectScore,
                totalPlayers
        );

        LevelManager.LevelResult levelResult = LevelManager.addXp(this, earnedXp);

        resultXp.setText("+" + levelResult.earnedXp + " XP");

        if (levelResult.isLevelUp()) {
            resultLevel.setText(
                    "Новый уровень! "
                            + levelResult.oldLevel
                            + " → "
                            + levelResult.newLevel
            );
        } else {
            resultLevel.setText(
                    "Уровень "
                            + levelResult.newLevel
                            + " • "
                            + levelResult.currentXp
                            + " / "
                            + levelResult.xpToNextLevel
                            + " XP"
            );
        }



        backToMainButton.setOnClickListener(v -> {
            Intent intent = new Intent(ResultActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}