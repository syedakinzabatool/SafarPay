package com.safarpay.ui.admin.settings;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.safarpay.R;
import com.safarpay.auth.LoginActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminSettingsFragment extends Fragment {
    private EditText etNoticeTitle;
    private EditText etNoticeBody;
    private RadioGroup rgNoticeType;
    private RadioGroup rgAudience;
    private Spinner spinnerNoticeUser;
    private ListView listNotices;
    private FirebaseFirestore db;
    private List<DocumentSnapshot> noticeDocs = new ArrayList<>();
    private final List<String> userLabels = new ArrayList<>();
    private final List<String> userUids = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle b) {
        return i.inflate(R.layout.fragment_admin_settings, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle s) {
        super.onViewCreated(view, s);
        db = FirebaseFirestore.getInstance();

        etNoticeTitle = view.findViewById(R.id.etNoticeTitle);
        rgNoticeType = view.findViewById(R.id.rgNoticeType);
        etNoticeBody = view.findViewById(R.id.etNoticeBody);
        rgAudience = view.findViewById(R.id.rgAudience);
        spinnerNoticeUser = view.findViewById(R.id.spinnerNoticeUser);
        listNotices = view.findViewById(R.id.listNotices);

        view.findViewById(R.id.btnPostNotice).setOnClickListener(v -> postNotice());
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getContext(), LoginActivity.class));
            requireActivity().finish();
        });

        rgAudience.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isSpecific = checkedId == R.id.rbAudienceSpecific;
            spinnerNoticeUser.setVisibility(isSpecific ? View.VISIBLE : View.GONE);
        });

        loadUsersForAudience();
        loadNotices();
    }

    private void loadUsersForAudience() {
        db.collection("users")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    userLabels.clear();
                    userUids.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String label = (name == null || name.trim().isEmpty())
                                ? String.valueOf(email)
                                : name + " (" + email + ")";
                        userLabels.add(label);
                        userUids.add(doc.getId());
                    }
                    spinnerNoticeUser.setAdapter(
                            new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, userLabels)
                    );
                });
    }

    private void postNotice() {
        String title = etNoticeTitle.getText().toString().trim();
        String body = etNoticeBody.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(body)) {
            Toast.makeText(getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isSpecificUser = rgAudience.getCheckedRadioButtonId() == R.id.rbAudienceSpecific;
        String targetUid = null;

        if (isSpecificUser) {
            int selected = spinnerNoticeUser.getSelectedItemPosition();
            if (selected < 0 || selected >= userUids.size()) {
                Toast.makeText(getContext(), "Select a user for targeted notification", Toast.LENGTH_SHORT).show();
                return;
            }
            targetUid = userUids.get(selected);
        }

        Map<String, Object> n = new HashMap<>();
        String type = "info";
        int sel = rgNoticeType.getCheckedRadioButtonId();
        if (sel == R.id.rbWarning) type = "warning";
        else if (sel == R.id.rbUrgent) type = "urgent";

        n.put("title", title);
        n.put("body", body);
        n.put("type", type);
        n.put("createdAt", System.currentTimeMillis());
        n.put("isActive", true);
        n.put("audience", isSpecificUser ? "user" : "all");
        n.put("targetUid", targetUid);

        db.collection("notices")
                .add(n)
                .addOnSuccessListener(r -> {
                    etNoticeTitle.setText("");
                    etNoticeBody.setText("");
                    loadNotices();
                    Toast.makeText(getContext(), isSpecificUser ? "Notification sent to selected user" : "Notice sent to all users", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadNotices() {
        db.collection("notices")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    noticeDocs = snap.getDocuments();
                    List<String> rows = new ArrayList<>();
                    for (DocumentSnapshot d : noticeDocs) {
                        String type = d.getString("type");
                        String icon = "ℹ";
                        if ("warning".equals(type)) icon = "⚠";
                        else if ("urgent".equals(type)) icon = "🚨";

                        String audience = d.getString("audience");
                        String audienceLabel;
                        if ("user".equals(audience)) {
                            String targetUid = d.getString("targetUid");
                            audienceLabel = "user:" + (targetUid == null ? "?" : targetUid);
                        } else {
                            audienceLabel = "all users";
                        }

                        rows.add(icon + " " + d.getString("title") + " [" + audienceLabel + "] - "
                                + (Boolean.TRUE.equals(d.getBoolean("isActive")) ? "ACTIVE" : "inactive"));
                    }

                    listNotices.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, rows));
                    listNotices.setOnItemClickListener((p, v, pos, id) -> {
                        DocumentSnapshot d = noticeDocs.get(pos);
                        boolean active = Boolean.TRUE.equals(d.getBoolean("isActive"));
                        d.getReference().update("isActive", !active).addOnSuccessListener(x -> loadNotices());
                    });
                });
    }
}