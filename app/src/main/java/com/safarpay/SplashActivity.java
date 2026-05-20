package com.safarpay;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.safarpay.auth.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            } else {
                checkRoleAndRoute(user.getUid());
            }
        }, 1800);
    }

    private void checkRoleAndRoute(String uid) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                        return;
                    }

                    if (!isAdmin(doc) && isTruthy(doc.get("disabled"))) {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                        return;
                    }

                    Intent intent;
                    if (doc.exists() && isAdmin(doc)) {
                        intent = new Intent(this, AdminActivity.class);
                    } else {
                        intent = new Intent(this, MainActivity.class);
                    }
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
    }

    /** Returns true only if role field is "admin" */
    private boolean isAdmin(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return false;
        }

        if (isAdminValue(doc.get("role")) || isAdminValue(doc.get("userType"))) {
            return true;
        }

        return isTruthy(doc.get("isAdmin")) || isTruthy(doc.get("admin"));
    }

    private boolean isAdminValue(Object value) {
        if (value == null) {
            return false;
        }
        return "admin".equalsIgnoreCase(String.valueOf(value).trim());
    }

    private boolean isTruthy(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value instanceof String) {
            String normalized = ((String) value).trim().toLowerCase();
            return "true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized) || "admin".equals(normalized);
        }
        return false;
    }
}