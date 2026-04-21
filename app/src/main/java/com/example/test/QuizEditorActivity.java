package com.example.test;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

public class QuizEditorActivity extends AppCompatActivity {

    private static final String KEY_QUIZZES = "QUIZZES_LIST";

    private long currentQuizId = -1;

    private static final String KEY_QUIZ_COUNT = "QUIZ_COUNT";

    private int countCreate;

    private int selectedQuestionIndex = -1;

    private LinearLayout questionsContainer;

    private LinearLayout questionNumbersContainer;
    private ScrollView scrollViewQuestions;

    private Button btnAddQuestion, btnSaveQuiz;

    private SharedPreferences preferences;
    private static final String PREF_NAME = "QUIZ_STORAGE";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.quiz_editor);
        currentQuizId = getIntent().getLongExtra("quizId", -1);

        ImageView closeEditorImg = findViewById(R.id.closeEditorImg);
        closeEditorImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();
            }
        });

        TextView closeEditor = findViewById(R.id.closeEditor);
        closeEditor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();
            }
        });

        questionsContainer = findViewById(R.id.questionsContainer);
        questionNumbersContainer = findViewById(R.id.questionNumbersContainer);
        scrollViewQuestions = findViewById(R.id.scrollViewQuestions);

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        countCreate = preferences.getInt(KEY_QUIZ_COUNT, 0);

        btnAddQuestion = findViewById(R.id.add_question);
        btnSaveQuiz = findViewById(R.id.save_quiz);


        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        btnAddQuestion.setOnClickListener(v -> {
            addQuestionView();
            selectedQuestionIndex = questionsContainer.getChildCount() - 1;
            updateQuestionNumbers();
            scrollToQuestion(selectedQuestionIndex);
        });

        btnSaveQuiz.setOnClickListener(v -> saveQuizLocally());

        loadQuizLocally();
        updateQuestionNumbers();
    }

    private void addQuestionView() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View questionView = inflater.inflate(R.layout.item_question, questionsContainer, false);

        Button btnAddAnswer = questionView.findViewById(R.id.addAnswer);
        Button btnDeleteQuestion = questionView.findViewById(R.id.dellQuestion);
        RadioGroup answersGroup = questionView.findViewById(R.id.answersRadioGroup);

        btnAddAnswer.setOnClickListener(v -> addAnswerView(answersGroup));

        btnDeleteQuestion.setOnClickListener(v -> {
            questionsContainer.removeView(questionView);
            updateQuestionNumbers();
        });

        questionsContainer.addView(questionView);
    }

    private void addAnswerView(RadioGroup group) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View answerView = inflater.inflate(R.layout.item_answer, group, false);

        ImageButton btnDeleteAnswer = answerView.findViewById(R.id.dellAnswer);

        btnDeleteAnswer.setOnClickListener(v -> group.removeView(answerView));

        group.addView(answerView);
    }

    private void updateQuestionNumbers() {
        questionNumbersContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < questionsContainer.getChildCount(); i++) {
            int index = i;

            View numberView = inflater.inflate(R.layout.item_question_number, questionNumbersContainer, false);
            TextView tvNumber = numberView.findViewById(R.id.tvNumber);

            tvNumber.setText(String.valueOf(i + 1));


            if (index == selectedQuestionIndex) {
                tvNumber.setBackgroundResource(R.drawable.bg_question_number_selected);
            } else {
                tvNumber.setBackgroundResource(R.drawable.bg_question_number);
            }

            tvNumber.setOnClickListener(v -> {
                selectedQuestionIndex = index;
                scrollToQuestion(index);
                updateQuestionNumbers(); // обновляем подсветку
            });

            questionNumbersContainer.addView(numberView);
        }
    }

    private void scrollToQuestion(int index) {
        if (index < 0 || index >= questionsContainer.getChildCount()) return;

        View targetQuestion = questionsContainer.getChildAt(index);

        scrollViewQuestions.post(() -> {
            scrollViewQuestions.smoothScrollTo(0, targetQuestion.getTop());
        });
    }

    private void saveQuizLocally() {
        try {
            JSONArray questionsArray = new JSONArray();


            for (int i = 0; i < questionsContainer.getChildCount(); i++) {
                View questionView = questionsContainer.getChildAt(i);

                EditText etQuestion = questionView.findViewById(R.id.question);
                RadioGroup answersGroup = questionView.findViewById(R.id.answersRadioGroup);

                String questionText = etQuestion.getText().toString().trim();

                if (questionText.isEmpty()) continue;

                JSONObject questionObj = new JSONObject();
                questionObj.put("question", questionText);

                JSONArray answersArray = new JSONArray();
                int correctIndex = -1;

                for (int j = 0; j < answersGroup.getChildCount(); j++) {
                    View answerView = answersGroup.getChildAt(j);

                    EditText etAnswer = answerView.findViewById(R.id.answer);
                    RadioButton rbCorrect = answerView.findViewById(R.id.radioCorrect);

                    String answerText = etAnswer.getText().toString().trim();

                    if (answerText.isEmpty()) continue;

                    answersArray.put(answerText);

                    if (rbCorrect.isChecked()) {
                        correctIndex = j;
                    }
                }


                if (answersArray.length() < 2) continue;

                questionObj.put("answers", answersArray);
                questionObj.put("correctIndex", correctIndex);

                questionsArray.put(questionObj);
            }

            if (questionsArray.length() == 0) {
                Toast.makeText(this, "Добавьте хотя бы один вопрос с ответами!", Toast.LENGTH_SHORT).show();
                return;
            }


            JSONObject currentQuiz = new JSONObject();

            if (currentQuizId == -1) {
                currentQuizId = System.currentTimeMillis();
            }

            currentQuiz.put("id", currentQuizId);
            currentQuiz.put("questions", questionsArray);


            String allQuizzesJson = preferences.getString(KEY_QUIZZES, "[]");
            JSONArray allQuizzesArray = new JSONArray(allQuizzesJson);


            boolean isUpdated = false;
            for (int i = 0; i < allQuizzesArray.length(); i++) {
                JSONObject quizObj = allQuizzesArray.getJSONObject(i);
                if (quizObj.getLong("id") == currentQuizId) {
                    allQuizzesArray.remove(i);
                    isUpdated = true;
                    break;
                }
            }


            if (!isUpdated) {
                currentQuiz.put("title", "Квиз #" + (allQuizzesArray.length() + 1));
            } else {

                currentQuiz.put("title", "Квиз (обновлён)");
            }


            allQuizzesArray.put(currentQuiz);


            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_QUIZZES, allQuizzesArray.toString());
            editor.putInt(KEY_QUIZ_COUNT, allQuizzesArray.length());
            editor.apply();

            Toast.makeText(this, "Квиз сохранён!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadQuizLocally() {
        try {

            if (currentQuizId == -1) return;

            String allQuizzesJson = preferences.getString(KEY_QUIZZES, "[]");
            JSONArray allQuizzesArray = new JSONArray(allQuizzesJson);

            JSONObject quizObj = null;

            for (int i = 0; i < allQuizzesArray.length(); i++) {
                JSONObject temp = allQuizzesArray.getJSONObject(i);
                if (temp.getLong("id") == currentQuizId) {
                    quizObj = temp;
                    break;
                }
            }

            if (quizObj == null) {
                Toast.makeText(this, "Квиз не найден!", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONArray questionsArray = quizObj.getJSONArray("questions");

            questionsContainer.removeAllViews();

            for (int i = 0; i < questionsArray.length(); i++) {
                JSONObject questionObj = questionsArray.getJSONObject(i);

                String questionText = questionObj.getString("question");
                JSONArray answersArray = questionObj.getJSONArray("answers");
                int correctIndex = questionObj.getInt("correctIndex");

                LayoutInflater inflater = LayoutInflater.from(this);
                View questionView = inflater.inflate(R.layout.item_question, questionsContainer, false);

                EditText etQuestion = questionView.findViewById(R.id.question);
                Button btnAddAnswer = questionView.findViewById(R.id.addAnswer);
                Button btnDeleteQuestion = questionView.findViewById(R.id.dellQuestion);
                RadioGroup answersGroup = questionView.findViewById(R.id.answersRadioGroup);

                etQuestion.setText(questionText);

                btnAddAnswer.setOnClickListener(v -> addAnswerView(answersGroup));

                btnDeleteQuestion.setOnClickListener(v -> {
                    questionsContainer.removeView(questionView);
                    updateQuestionNumbers();
                });

                for (int j = 0; j < answersArray.length(); j++) {
                    View answerView = inflater.inflate(R.layout.item_answer, answersGroup, false);

                    RadioButton rbCorrect = answerView.findViewById(R.id.radioCorrect);
                    EditText etAnswer = answerView.findViewById(R.id.answer);
                    ImageButton btnDeleteAnswer = answerView.findViewById(R.id.dellAnswer);

                    etAnswer.setText(answersArray.getString(j));

                    if (j == correctIndex) {
                        rbCorrect.setChecked(true);
                    }

                    btnDeleteAnswer.setOnClickListener(v -> answersGroup.removeView(answerView));

                    answersGroup.addView(answerView);
                }

                questionsContainer.addView(questionView);
            }

            updateQuestionNumbers();

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
