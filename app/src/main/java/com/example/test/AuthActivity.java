package com.example.test;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.example.test.models.LoginRequest;
import com.example.test.models.SignupRequest;
import com.example.test.models.UserResponse;

public class AuthActivity extends AppCompatActivity {


    private TextView btnLogin, btnRegister;
    private Button btnAction;
    private LinearLayout buttonHome;

    private EditText etUsername, etEmail, etPassword;

    private boolean isRegisterMode = false;

    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        ConstraintLayout constraintLayout = findViewById(R.id.constraintLayout);

        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnAction = findViewById(R.id.btn_login);

        buttonHome= findViewById(R.id.buttonHome);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.Email);
        etPassword = findViewById(R.id.Password);

        api = ApiClient.getClient().create(ApiService.class);

        setLoginMode();

        btnLogin.setOnClickListener(v -> setLoginMode());
        btnRegister.setOnClickListener(v -> setRegisterMode());
        buttonHome.setOnClickListener( view -> {setContentView(R.layout.activity_main);});

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

        etUsername.setVisibility(View.VISIBLE);
        etEmail.setVisibility(View.VISIBLE);
        btnAction.setText("Зарегистрироваться");

        ConstraintLayout constraintLayout = findViewById(R.id.constraintLayout);

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        int marginInDp = 60;
        float scale = getResources().getDisplayMetrics().density;
        int marginInPx = (int) (marginInDp * scale + 0.5f);

        constraintSet.setMargin(R.id.Email, ConstraintSet.TOP, marginInPx);

        constraintSet.applyTo(constraintLayout);
    }

    private void registerUser() {

        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля!", Toast.LENGTH_SHORT).show();
            return;
        }

        SignupRequest request = new SignupRequest(username, email, password);

        ApiService api = ApiClient.getClient().create(ApiService.class);

        api.signup(request).enqueue(new retrofit2.Callback<UserResponse>() {
            @Override
            public void onResponse(retrofit2.Call<UserResponse> call, retrofit2.Response<UserResponse> response) {

                if (response.isSuccessful() && response.body() != null) {

                    UserResponse user = response.body();

                    SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
                    prefs.edit()
                            .putLong("USER_ID", user.id)
                            .putString("USERNAME", user.username)
                            .putString("EMAIL", user.email)
                            .putBoolean("isLogin", true)
                            .apply();

                    Toast.makeText(AuthActivity.this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(AuthActivity.this, "Ошибка регистрации: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<UserResponse> call, Throwable t) {
                Toast.makeText(AuthActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loginUser() {

        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Введите логин и пароль!", Toast.LENGTH_SHORT).show();
            return;
        }

        LoginRequest request = new LoginRequest(username, password);

        ApiService api = ApiClient.getClient().create(ApiService.class);

        api.login(request).enqueue(new retrofit2.Callback<UserResponse>() {
            @Override
            public void onResponse(retrofit2.Call<UserResponse> call, retrofit2.Response<UserResponse> response) {

                if (response.isSuccessful() && response.body() != null) {

                    UserResponse user = response.body();

                    SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
                    prefs.edit()
                            .putBoolean("isLogin", true)
                            .putLong("USER_ID", user.id)
                            .putString("USERNAME", user.username)
                            .putString("EMAIL", user.email)
                            .apply();

                    Toast.makeText(AuthActivity.this, "Вход выполнен!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(AuthActivity.this, "Неверный логин или пароль!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<UserResponse> call, Throwable t) {
                Toast.makeText(AuthActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}