package com.safarpay;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatDelegate;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.safarpay.auth.LoginActivity;

public class MainActivity extends AppCompatActivity {

    private static final String APP_TITLE = "SafarPay ✈";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        View backButton = findViewById(R.id.btnNavigateBack);
        View converterButton = findViewById(R.id.btnOpenConverter);
        android.widget.TextView titleView = findViewById(R.id.tvToolbarTitle);

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                NavigationUI.setupWithNavController(bottomNav, navController);
            }

            navController.addOnDestinationChangedListener((ctrl, dest, args) -> {
                boolean isHome = dest.getId() == R.id.homeFragment;
                titleView.setVisibility(isHome ? View.VISIBLE : View.GONE);
                titleView.setText(APP_TITLE);
                backButton.setVisibility(isHome ? View.GONE : View.VISIBLE);
                converterButton.setVisibility(isHome ? View.VISIBLE : View.GONE);
            });

            backButton.setOnClickListener(v -> {
                if (!navController.popBackStack(R.id.homeFragment, false)) {
                    navController.navigate(R.id.homeFragment);
                }
            });



            // Compass button → open Currency Converter
            converterButton.setOnClickListener(v -> {
                int currentDest = 0;
                if (navController.getCurrentDestination() != null)
                    currentDest = navController.getCurrentDestination().getId();
                if (currentDest != R.id.converterFragment) {
                    navController.navigate(R.id.converterFragment);
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        enforceUserAccountState();
    }

    private void enforceUserAccountState() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            goToLogin();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    boolean missing = !doc.exists();
                    boolean admin = isAdmin(doc);
                    boolean disabled = doc.exists() && !admin && Boolean.TRUE.equals(doc.getBoolean("disabled"));
                    boolean disabledByAdmin = doc.exists() && Boolean.TRUE.equals(doc.getBoolean("disabledByAdmin"));
                    if (missing || disabled) {
                        FirebaseAuth.getInstance().signOut();
                        String msg;
                        if (missing) {
                            msg = "Account not found. Contact support.";
                        } else if (disabledByAdmin) {
                            msg = "Your account has been deactivated by admin";
                        } else {
                            msg = "Your account access is unavailable";
                        }
                        Toast.makeText(this,
                                msg,
                                Toast.LENGTH_LONG).show();
                        goToLogin();
                    }
                })
                .addOnFailureListener(e -> {
                    FirebaseAuth.getInstance().signOut();
                    goToLogin();
                });
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private boolean isAdmin(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return false;

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