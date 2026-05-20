package com.safarpay.ui.admin.dashboard;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;
import com.safarpay.AdminActivity;
import com.safarpay.R;
import java.util.*;

public class AdminDashboardFragment extends Fragment {

    public interface OnTileClickListener {
        void onUsersTileClicked();
        void onTripsTileClicked();
    }

    private TextView tvUserCount, tvTripCount, tvRecentActivityEmpty;
    private RecyclerView recyclerRecentSignups;
    private RecentSignupAdapter recentSignupAdapter;
    private final List<Map<String, Object>> recentSignups = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle b) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();

        tvUserCount    = view.findViewById(R.id.tvUserCount);
        tvTripCount    = view.findViewById(R.id.tvTripCount);
        tvRecentActivityEmpty = view.findViewById(R.id.tvRecentActivityEmpty);
        recyclerRecentSignups = view.findViewById(R.id.recyclerRecentSignups);
        recyclerRecentSignups.setLayoutManager(new LinearLayoutManager(getContext()));
        recentSignupAdapter = new RecentSignupAdapter(recentSignups, this::deleteUser);
        recyclerRecentSignups.setAdapter(recentSignupAdapter);

        // Tiles navigate to respective admin sections
        CardView cardUsers = view.findViewById(R.id.cardUsers);
        CardView cardTrips = view.findViewById(R.id.cardTrips);
        if (cardUsers != null) {
            cardUsers.setOnClickListener(v -> {
                if (getActivity() instanceof AdminActivity)
                    ((AdminActivity) getActivity()).navigateToSection(R.id.nav_users);
            });
        }
        if (cardTrips != null) {
            cardTrips.setOnClickListener(v -> {
                if (getActivity() instanceof AdminActivity)
                    ((AdminActivity) getActivity()).navigateToSection(R.id.nav_trips);
            });
        }

        view.findViewById(R.id.btnManageUsers).setOnClickListener(v -> {
            if (getActivity() instanceof AdminActivity)
                ((AdminActivity) getActivity()).navigateToSection(R.id.nav_users);
        });

        loadStats();
    }

    private void loadStats() {
        db.collection("users").get().addOnSuccessListener(snap -> {
            if (isAdded()) tvUserCount.setText(String.valueOf(snap.size()));
        });

        db.collection("trips").get().addOnSuccessListener(snap -> {
            if (isAdded()) {
                tvTripCount.setText(String.valueOf(snap.size()));
            }
        });
    }

    private void loadRecentSignups() {
        db.collection("users")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(8)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    recentSignups.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Map<String, Object> user = new HashMap<>();
                        if (doc.getData() != null) {
                            user.putAll(doc.getData());
                        }
                        user.put("uid", doc.getId());
                        recentSignups.add(user);
                    }
                    recentSignupAdapter.notifyDataSetChanged();
                    boolean isEmpty = recentSignups.isEmpty();
                    recyclerRecentSignups.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                    tvRecentActivityEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                });
    }

    private void deleteUser(Map<String, Object> user) {
        if (getContext() == null) return;

        String uid = String.valueOf(user.get("uid"));
        String name = String.valueOf(user.getOrDefault("name", "this user"));

        new AlertDialog.Builder(requireContext())
                .setTitle("Remove user")
                .setMessage("Delete " + name + " and related Firestore data?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteUserFromFirestore(uid))
                .show();
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
                                    Toast.makeText(requireContext(), "User and related data removed", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(requireContext(), "User removed with partial cleanup", Toast.LENGTH_LONG).show();
                                }
                                loadStats();
                            })
                            .addOnFailureListener(e -> {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), "Could not remove user", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Could not remove user", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}