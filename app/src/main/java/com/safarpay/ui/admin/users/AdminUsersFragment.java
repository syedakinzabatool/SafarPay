package com.safarpay.ui.admin.users;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.safarpay.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminUsersFragment extends Fragment {
    private RecyclerView recyclerView;
    private EditText etSearch;
    private final List<Map<String, Object>> userList = new ArrayList<>();
    private UserAdapter adapter;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater i, ViewGroup c, Bundle b) {
        return i.inflate(R.layout.fragment_admin_users, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle s) {
        super.onViewCreated(view, s);
        db = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.recyclerUsers);
        etSearch = view.findViewById(R.id.etSearch);

        adapter = new UserAdapter(userList, new UserAdapter.OnUserActionListener() {
            @Override
            public void onToggle(String uid, boolean currentlyDisabled, String displayName) {
                confirmAndToggleUser(uid, currentlyDisabled, displayName);
            }

            @Override
            public void onDelete(String uid, String displayName) {
                confirmAndDeleteUser(uid, displayName);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s2, int a, int b2, int c2) {}

            @Override
            public void afterTextChanged(Editable s2) {}

            @Override
            public void onTextChanged(CharSequence s2, int a, int b2, int c2) {
                adapter.filter(s2.toString());
            }
        });

        loadUsers();
    }

    private void loadUsers() {
        db.collection("users").get().addOnSuccessListener(snap -> {
            userList.clear();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Map<String, Object> source = doc.getData();
                Map<String, Object> user = (source == null) ? new HashMap<>() : new HashMap<>(source);
                user.put("uid", doc.getId());
                userList.add(user);
            }
            adapter.setData(userList);
        });
    }

    private void confirmAndToggleUser(String uid, boolean currentlyDisabled, String displayName) {
        if (getContext() == null) return;
        String action = currentlyDisabled ? "activate" : "deactivate";
        String titleAction = currentlyDisabled ? "Activate" : "Deactivate";

        new AlertDialog.Builder(requireContext())
                .setTitle(titleAction + " User")
                .setMessage("Are you sure you want to " + action + " " + displayName + "?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton(titleAction, (d, which) -> toggleUser(uid, currentlyDisabled))
                .show();
    }

    private void confirmAndDeleteUser(String uid, String displayName) {
        if (getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete User")
                .setMessage("Delete " + displayName + " permanently? This cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, which) -> deleteUser(uid))
                .show();
    }

    private void toggleUser(String uid, boolean currentlyDisabled) {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                if (isAdded()) Snackbar.make(requireView(), "User not found", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (isAdmin(doc)) {
                if (isAdded()) Snackbar.make(requireView(), "Admin account is protected", Snackbar.LENGTH_SHORT).show();
                return;
            }
            db.collection("users").document(uid)
                    .update(
                        "disabled", !currentlyDisabled,
                        "disabledByAdmin", !currentlyDisabled
                    )
                    .addOnSuccessListener(v -> {
                        if (!isAdded()) return;
                        Snackbar.make(requireView(), currentlyDisabled ? "User activated" : "User deactivated", Snackbar.LENGTH_SHORT).show();
                        loadUsers();
                    });
        });
    }

    private void deleteUser(String uid) {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                if (isAdded()) Snackbar.make(requireView(), "User not found", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (isAdmin(doc)) {
                if (isAdded()) Snackbar.make(requireView(), "Admin account is protected", Snackbar.LENGTH_SHORT).show();
                return;
            }
            deleteUserFromFirestore(uid);
        });
    }

    private void deleteUserFromFirestore(String uid) {
        Task<QuerySnapshot> tripsTask = db.collection("trips")
                .whereEqualTo("userId", uid)
                .get();

        Task<QuerySnapshot> noticesTask = db.collection("notices")
                .whereEqualTo("targetUid", uid)
                .get();

        Tasks.whenAllSuccess(tripsTask, noticesTask)
                .addOnSuccessListener(results -> {
                    QuerySnapshot trips = (QuerySnapshot) results.get(0);
                    QuerySnapshot notices = (QuerySnapshot) results.get(1);

                    List<Task<Void>> deleteTasks = new ArrayList<>();
                    for (DocumentSnapshot tripDoc : trips.getDocuments()) {
                        Task<Void> tripCascadeDelete = tripDoc.getReference()
                                .collection("expenses")
                                .get()
                                .continueWithTask(expensesTask -> {
                                    List<Task<Void>> nestedDeletes = new ArrayList<>();
                                    if (expensesTask.isSuccessful() && expensesTask.getResult() != null) {
                                        for (DocumentSnapshot expenseDoc : expensesTask.getResult().getDocuments()) {
                                            nestedDeletes.add(expenseDoc.getReference().delete());
                                        }
                                    }
                                    nestedDeletes.add(tripDoc.getReference().delete());
                                    return Tasks.whenAllComplete(nestedDeletes).continueWith(t -> null);
                                });
                        deleteTasks.add(tripCascadeDelete);
                    }
                    for (DocumentSnapshot noticeDoc : notices.getDocuments()) {
                        deleteTasks.add(noticeDoc.getReference().delete());
                    }

                    deleteTasks.add(db.collection("users").document(uid).delete());

                    Tasks.whenAllComplete(deleteTasks)
                            .addOnSuccessListener(done -> {
                                int failed = 0;
                                for (Task<?> t : done) {
                                    if (!t.isSuccessful()) failed++;
                                }

                                if (!isAdded()) return;
                                if (failed == 0) {
                                    Snackbar.make(requireView(), "User and related Firestore data deleted", Snackbar.LENGTH_SHORT).show();
                                } else {
                                    Snackbar.make(requireView(), "User deleted with partial cleanup", Snackbar.LENGTH_LONG).show();
                                }
                                loadUsers();
                            })
                            .addOnFailureListener(e -> {
                                if (isAdded()) {
                                    Snackbar.make(requireView(), "Could not complete deletion", Snackbar.LENGTH_LONG).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Snackbar.make(requireView(), "Could not load related user data", Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private boolean isAdmin(DocumentSnapshot doc) {
        String role = doc.getString("role");
        return role != null && "admin".equalsIgnoreCase(role.trim());
    }
}