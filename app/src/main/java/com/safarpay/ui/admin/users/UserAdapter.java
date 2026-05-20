package com.safarpay.ui.admin.users;

import android.graphics.Color;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.safarpay.R;
import java.util.*;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {

    public interface OnUserActionListener {
        void onToggle(String uid, boolean currentlyDisabled, String displayName);
        void onDelete(String uid, String displayName);
    }

    private List<Map<String, Object>> data = new ArrayList<>(), filtered = new ArrayList<>();
    private final OnUserActionListener listener;

    public UserAdapter(List<Map<String, Object>> d, OnUserActionListener l) {
        this.data = d; this.filtered = new ArrayList<>(d); this.listener = l;
    }

    public void setData(List<Map<String, Object>> list) {
        data = list; filtered = new ArrayList<>(list); notifyDataSetChanged();
    }

    public void filter(String q) {
        filtered.clear();
        for (Map<String, Object> u : data) {
            String n = String.valueOf(u.getOrDefault("name", ""));
            String e = String.valueOf(u.getOrDefault("email", ""));
            if (n.toLowerCase().contains(q.toLowerCase()) || e.toLowerCase().contains(q.toLowerCase()))
                filtered.add(u);
        }
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_user_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Map<String, Object> u = filtered.get(pos);
        String name  = String.valueOf(u.getOrDefault("name", "?"));
        String email = String.valueOf(u.getOrDefault("email", ""));
        String uid = String.valueOf(u.getOrDefault("uid", ""));
        boolean disabled = Boolean.TRUE.equals(u.getOrDefault("disabled", false));
        String role = String.valueOf(u.getOrDefault("role", ""));
        boolean admin = "admin".equalsIgnoreCase(role.trim());

        h.tvName.setText(name);
        h.tvEmail.setText(email);
        h.tvAvatar.setText(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase());

        if (admin) {
            h.tvStatus.setText("ADMIN");
            h.tvStatus.setTextColor(Color.parseColor("#0D47A1"));
            h.tvStatus.setBackgroundColor(Color.parseColor("#E3F2FD"));
        } else if (disabled) {
            h.tvStatus.setText("DISABLED");
            h.tvStatus.setTextColor(Color.parseColor("#C62828"));
            h.tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
        } else {
            h.tvStatus.setText("ACTIVE");
            h.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
            h.tvStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
        }

        if (admin) {
            h.btnToggle.setText("Protected");
            h.btnToggle.setEnabled(false);
            h.btnDelete.setEnabled(false);
            h.btnDelete.setAlpha(0.5f);
            h.btnToggle.setAlpha(0.5f);
            h.btnToggle.setOnClickListener(null);
            h.btnDelete.setOnClickListener(null);
        } else {
            h.btnToggle.setAlpha(1f);
            h.btnDelete.setAlpha(1f);
            h.btnToggle.setEnabled(true);
            h.btnDelete.setEnabled(true);
            h.btnToggle.setText(disabled ? "Activate" : "Deactivate");
            h.btnToggle.setOnClickListener(v -> listener.onToggle(uid, disabled, name));
            h.btnDelete.setOnClickListener(v -> listener.onDelete(uid, name));
        }
    }

    @Override public int getItemCount() { return filtered.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvStatus, tvAvatar;
        Button btnToggle, btnDelete;
        VH(View v) {
            super(v);
            tvName   = v.findViewById(R.id.tvUserName);
            tvEmail  = v.findViewById(R.id.tvUserEmail);
            tvStatus = v.findViewById(R.id.tvUserStatus);
            tvAvatar = v.findViewById(R.id.tvAvatarLetter);
            btnToggle = v.findViewById(R.id.btnToggleUser);
            btnDelete = v.findViewById(R.id.btnDeleteUser);
        }
    }
}
