package com.campusride.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etPhone;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnPrimaryAuth;
    private TextView tvTabLogin;
    private TextView tvTabRegister;
    private TextView tvAuthTitle;
    private TextView tvAuthSubtitle;
    private TextView tvSwitchPrompt;
    private TextView tvSwitchAction;
    private LinearLayout layoutRegisterFields;

    private FirebaseHelper firebaseHelper;
    private SessionManager sessionManager;
    private boolean isRegisterMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnPrimaryAuth = findViewById(R.id.btnPrimaryAuth);
        tvTabLogin = findViewById(R.id.tvTabLogin);
        tvTabRegister = findViewById(R.id.tvTabRegister);
        tvAuthTitle = findViewById(R.id.tvAuthTitle);
        tvAuthSubtitle = findViewById(R.id.tvAuthSubtitle);
        tvSwitchPrompt = findViewById(R.id.tvSwitchPrompt);
        tvSwitchAction = findViewById(R.id.tvSwitchAction);
        layoutRegisterFields = findViewById(R.id.layoutRegisterFields);

        firebaseHelper = FirebaseHelper.getInstance();
        sessionManager = new SessionManager(this);

        if (firebaseHelper.isLoggedIn()) {
            openHomeScreen();
            return;
        }

        etEmail.setText(sessionManager.getSavedEmail());
        setupModeSwitching();
        showLoginMode();

        btnPrimaryAuth.setOnClickListener(v -> {
            if (isRegisterMode) {
                registerUser();
            } else {
                loginUser();
            }
        });
    }

    private void setupModeSwitching() {
        tvTabLogin.setOnClickListener(v -> showLoginMode());
        tvTabRegister.setOnClickListener(v -> showRegisterMode());
        tvSwitchAction.setOnClickListener(v -> {
            if (isRegisterMode) {
                showLoginMode();
            } else {
                showRegisterMode();
            }
        });
    }

    private void showLoginMode() {
        isRegisterMode = false;
        layoutRegisterFields.setVisibility(LinearLayout.GONE);
        tvAuthTitle.setText("Welcome back");
        tvAuthSubtitle.setText("Login to continue managing your campus rides.");
        btnPrimaryAuth.setText("Login");
        tvSwitchPrompt.setText("New here?");
        tvSwitchAction.setText("Create account");
        tvTabLogin.setBackgroundResource(R.drawable.bg_auth_tab_active);
        tvTabRegister.setBackgroundResource(R.drawable.bg_auth_tab_inactive);
        tvTabLogin.setTextColor(getColor(android.R.color.white));
        tvTabRegister.setTextColor(getColor(R.color.text_secondary));
    }

    private void showRegisterMode() {
        isRegisterMode = true;
        layoutRegisterFields.setVisibility(LinearLayout.VISIBLE);
        tvAuthTitle.setText("Create your account");
        tvAuthSubtitle.setText("Set up your rider profile and start sharing trips.");
        btnPrimaryAuth.setText("Create account");
        tvSwitchPrompt.setText("Already have an account?");
        tvSwitchAction.setText("Login");
        tvTabLogin.setBackgroundResource(R.drawable.bg_auth_tab_inactive);
        tvTabRegister.setBackgroundResource(R.drawable.bg_auth_tab_active);
        tvTabLogin.setTextColor(getColor(R.color.text_secondary));
        tvTabRegister.setTextColor(getColor(android.R.color.white));
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
