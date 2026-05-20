package com.safarpay.ui.home;

import android.app.Application;
import androidx.lifecycle.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.safarpay.data.local.entity.Trip;
import com.safarpay.data.repository.ExpenseRepository;
import com.safarpay.data.repository.TripRepository;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final TripRepository tripRepo;
    private final ExpenseRepository expenseRepo;
    private final LiveData<Trip> activeTrip;
    private final MutableLiveData<String> notice = new MutableLiveData<>();
    private ListenerRegistration noticeListener;

    public HomeViewModel(Application app) {
        super(app);
        tripRepo    = new TripRepository(app);
        expenseRepo = new ExpenseRepository(app);
        activeTrip  = tripRepo.getActiveTrip();
        listenForNotice();
    }

    public LiveData<Trip> getActiveTrip() { return activeTrip; }
    public MutableLiveData<String> getNotice() { return notice; }

    public LiveData<Double> getTotalSpentPKR(int tripId) {
        return expenseRepo.getTotalSpentPKR(tripId);
    }

    private void listenForNotice() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = user != null ? user.getUid() : null;

        noticeListener = FirebaseFirestore.getInstance()
                .collection("notices")
                .whereEqualTo("isActive", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(25)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || snap.isEmpty()) {
                        notice.postValue(null);
                        return;
                    }
                    notice.postValue(selectNoticeMessageForUser(snap.getDocuments(), currentUid));
                });
    }

    private String selectNoticeMessageForUser(List<DocumentSnapshot> docs, String currentUid) {
        String broadcastMessage = null;

        for (DocumentSnapshot doc : docs) {
            String audience = doc.getString("audience");
            String targetUid = doc.getString("targetUid");

            // Backward compatibility: old notices without audience are treated as broadcast.
            boolean isBroadcast = audience == null || "all".equals(audience);
            boolean isTargetedToCurrent = "user".equals(audience)
                    && currentUid != null
                    && currentUid.equals(targetUid);

            if (isTargetedToCurrent) {
                return extractNoticeMessage(doc);
            }
            if (isBroadcast && broadcastMessage == null) {
                broadcastMessage = extractNoticeMessage(doc);
            }
        }

        return broadcastMessage;
    }

    private String extractNoticeMessage(DocumentSnapshot doc) {
        String type = doc.getString("type");
        String prefix = "ℹ Notice";
        if ("warning".equals(type)) prefix = "⚠ Warning";
        else if ("urgent".equals(type)) prefix = "🚨 Urgent";

        String title = doc.getString("title");
        String body = doc.getString("body");
        StringBuilder message = new StringBuilder(prefix);
        if (title != null && !title.trim().isEmpty()) {
            message.append(": ").append(title.trim());
        }
        if (body != null && !body.trim().isEmpty()) {
            if (title != null && !title.trim().isEmpty()) {
                message.append(" - ");
            } else {
                message.append(": ");
            }
            message.append(body.trim());
        }
        return message.toString();
    }

    @Override
    protected void onCleared() {
        if (noticeListener != null) noticeListener.remove();
        super.onCleared();
    }
}
