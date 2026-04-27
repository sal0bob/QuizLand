package com.example.test;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class AuthActivity extends AppCompatActivity {

    private boolean isLogin = false;

    private TextView btnLogin, btnRegister;
    private EditText etLogin, etPassword;
    private Button btnAction;

    private boolean isRegisterMode = false;

    private SharedPreferences preferences;
    private static final String PREF_NAME = "USER_DATA";
    private static final String KEY_LOGIN = "LOGIN";
    private static final String KEY_PASSWORD = "PASSWORD";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("isLogin", isLogin).apply();

        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        etLogin = findViewById(R.id.Email);
        etPassword = findViewById(R.id.Password);

        btnAction = findViewById(R.id.btn_login);

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        setLoginMode();

        btnLogin.setOnClickListener(v -> setLoginMode());
        btnRegister.setOnClickListener(v -> setRegisterMode());

        btnAction.setOnClickListener(v -> {
            if (isRegisterMode) {
                registerUser();
            } else {
                loginUser();
            }
        });
    }

    private void setLoginMode() {
        isRegisterMode = false;
        btnAction.setText("Войти");
    }

    private void setRegisterMode() {
        isRegisterMode = true;
        btnAction.setText("Зарегистрироваться");
    }

    private void registerUser() {
        String login = etLogin.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (login.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Введите логин и пароль!", Toast.LENGTH_SHORT).show();
            return;
        }

        preferences.edit()
                .putString(KEY_LOGIN, login)
                .putString(KEY_PASSWORD, password)
                .apply();

        Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();

        setLoginMode();
    }

    private void loginUser() {
        String login = etLogin.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        String savedLogin = preferences.getString(KEY_LOGIN, null);
        String savedPassword = preferences.getString(KEY_PASSWORD, null);

        if (savedLogin == null || savedPassword == null) {
            Toast.makeText(this, "Сначала зарегистрируйтесь!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (login.equals(savedLogin) && password.equals(savedPassword)) {
            Toast.makeText(this, "Вход успешен!", Toast.LENGTH_SHORT).show();

            isLogin = true;

            SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
            prefs.edit().putBoolean("isLogin", isLogin).apply();

            Intent intent = new Intent(AuthActivity.this, MainActivity.class);
            startActivity(intent);
            finish();

        } else {
            Toast.makeText(this, "Неверный логин или пароль!", Toast.LENGTH_SHORT).show();
        }
    }
}