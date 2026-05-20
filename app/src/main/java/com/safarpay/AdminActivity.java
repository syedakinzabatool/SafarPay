package com.safarpay;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.Toast;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.navigation.NavigationView;
import com.safarpay.auth.LoginActivity;
import com.safarpay.ui.admin.analytics.AdminAnalyticsFragment;
import com.safarpay.ui.admin.dashboard.AdminDashboardFragment;
import com.safarpay.ui.admin.settings.AdminSettingsFragment;
import com.safarpay.ui.admin.trips.AdminTripsFragment;
import com.safarpay.ui.admin.users.AdminUsersFragment;

public class AdminActivity extends AppCompatActivity
        implements AdminDashboardFragment.OnTileClickListener {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private int currentMenuId = R.id.nav_dashboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(null);
            getSupportActionBar().setSubtitle(null);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        navView      = findViewById(R.id.nav_view);

        enforceAdminAccess();

        updateToolbarForSection(R.id.nav_dashboard);

        navView.setNavigationItemSelectedListener(item -> {
            navigateToSection(item.getItemId());
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        if (savedInstanceState == null) {
            loadFragment(new AdminDashboardFragment(), R.id.nav_dashboard);
        }
    }

    /** Kicks out anyone whose role is not admin. */
    private void enforceAdminAccess() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists() || !isAdmin(doc)) {
                        Toast.makeText(this, "Admin access required", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Unable to verify admin access. Opening the app instead.", Toast.LENGTH_SHORT).show();
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

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    public void navigateToSection(int menuItemId) {
        Fragment fragment = null;
        if      (menuItemId == R.id.nav_dashboard)  fragment = new AdminDashboardFragment();
        else if (menuItemId == R.id.nav_users)       fragment = new AdminUsersFragment();
        else if (menuItemId == R.id.nav_trips)       fragment = new AdminTripsFragment();
        else if (menuItemId == R.id.nav_analytics)   fragment = new AdminAnalyticsFragment();
        else if (menuItemId == R.id.nav_settings)    fragment = new AdminSettingsFragment();
        if (fragment != null) loadFragment(fragment, menuItemId);
    }

    private void loadFragment(Fragment fragment, int menuId) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.admin_content_frame, fragment).commit();
        currentMenuId = menuId;
        updateToolbarForSection(menuId);
        navView.setCheckedItem(menuId);
    }

    private void updateToolbarForSection(int menuId) {
        if (toolbar == null) return;

        if (menuId == R.id.nav_dashboard) {
            toolbar.setNavigationIcon(R.drawable.ic_menu_admin);
            toolbar.setNavigationContentDescription(R.string.navigation_drawer_open);
            toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_teal);
            toolbar.setNavigationContentDescription(R.string.navigation_drawer_close);
            toolbar.setNavigationOnClickListener(v -> navigateToSection(R.id.nav_dashboard));
        }
    }

    @Override public void onUsersTileClicked() { navigateToSection(R.id.nav_users); }
    @Override public void onTripsTileClicked()  { navigateToSection(R.id.nav_trips); }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (currentMenuId != R.id.nav_dashboard) {
            navigateToSection(R.id.nav_dashboard);
        } else {
            super.onBackPressed();
        }
    }
}