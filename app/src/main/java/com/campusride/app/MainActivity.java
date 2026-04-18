package com.campusride.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etName, etPhone, etEmail, etPassword;
    private Button btnRegister, btnLogin;
    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnLogin = findViewById(R.id.btnLogin);

        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = new SessionManager(this);

        if (firebaseHelper.isLoggedIn() || sessionManager.hasActiveSession()) {
            openHomeScreen();
            return;
        }

        etEmail.setText(sessionManager.getSavedEmail());

        btnRegister.setOnClickListener(v -> registerUser());
        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Enter name");
            etName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Enter phone number");
            etPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter password");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        firebaseHelper.registerUser(email, password, name, phone, task -> {
            if (task.isSuccessful()) {
                sessionManager.saveLoginSession(email);
                Toast.makeText(MainActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                openHomeScreen();
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter email");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter password");
            etPassword.requestFocus();
            return;
        }

        firebaseHelper.loginUser(email, password, task -> {
            if (task.isSuccessful()) {
                sessionManager.saveLoginSession(email);
                Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                openHomeScreen();
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "Login failed";
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openHomeScreen() {
        startActivity(new Intent(MainActivity.this, HomeActivity.class));
        finish();
    }
}
