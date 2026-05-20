package com.safarpay.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.appcompat.app.AppCompatDelegate;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.safarpay.*;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoogleSignIn;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        btnLogin    = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        progressBar = findViewById(R.id.progressBar);

        TextView tvRegister = findViewById(R.id.tvRegister);
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnGoogleSignIn.setOnClickListener(v -> attemptGoogleLogin());
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String pass  = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        setLoading(false);
                        Toast.makeText(this, "Login failed. Please try again.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    routeSignedInUser(user, false);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void attemptGoogleLogin() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.google_web_client_id))
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        setLoading(true);
        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        handleGoogleCredentialResponse(response);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this,
                                "Google sign-in cancelled or failed",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void handleGoogleCredentialResponse(GetCredentialResponse response) {
        Credential credential = response.getCredential();
        if (!(credential instanceof CustomCredential)) {
            setLoading(false);
            Toast.makeText(this, "Unsupported Google credential", Toast.LENGTH_SHORT).show();
            return;
        }

        CustomCredential customCredential = (CustomCredential) credential;
        if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(customCredential.getType())) {
            setLoading(false);
            Toast.makeText(this, "Invalid Google credential type", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            GoogleIdTokenCredential googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(customCredential.getData());
            String idToken = googleIdTokenCredential.getIdToken();
            if (idToken != null) {
                firebaseAuthWithGoogle(idToken);
            } else {
                setLoading(false);
                Toast.makeText(this, "Failed to extract Google token", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            setLoading(false);
            Toast.makeText(this, "Could not parse Google credentials", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(firebaseCredential)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        setLoading(false);
                        Toast.makeText(this, "Google login failed. Try again.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    routeSignedInUser(user, true);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Google login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void routeSignedInUser(FirebaseUser user, boolean createIfMissing) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        if (createIfMissing) {
                            createUserDocForSignedInUser(user);
                        } else {
                            routeToMainWithWarning("Profile not found. Opening the app anyway.");
                        }
                        return;
                    }

                    setLoading(false);
                    boolean admin = isAdmin(doc);
                    boolean disabled = Boolean.TRUE.equals(doc.getBoolean("disabled"));
                    boolean disabledByAdmin = Boolean.TRUE.equals(doc.getBoolean("disabledByAdmin"));
                    if (!admin && disabled) {
                        FirebaseAuth.getInstance().signOut();
                        String msg = disabledByAdmin
                                ? "Your account has been deactivated by admin"
                                : "Your account access is unavailable";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (admin) {
                        startActivity(new Intent(this, AdminActivity.class));
                    } else {
                        startActivity(new Intent(this, MainActivity.class));
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    routeToMainWithWarning("Could not verify your profile right now. Opening the app.");
                });
    }

    private void createUserDocForSignedInUser(FirebaseUser user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", user.getDisplayName() == null ? "User" : user.getDisplayName());
        profile.put("email", user.getEmail() == null ? "" : user.getEmail());
        profile.put("role", "user");
        profile.put("createdAt", System.currentTimeMillis());
        profile.put("disabled", false);
        profile.put("disabledByAdmin", false);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    routeToMainWithWarning("Profile setup could not be saved. Opening the app anyway.");
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

        Object isAdmin = doc.get("isAdmin");
        Object admin = doc.get("admin");
        return isTruthy(isAdmin) || isTruthy(admin);
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

    private void routeToMainWithWarning(String message) {
        setLoading(false);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setEnabled(!loading);
        }
    }
}