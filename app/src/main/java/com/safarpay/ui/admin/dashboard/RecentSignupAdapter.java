package com.safarpay.ui.admin.dashboard;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.safarpay.R;
import java.util.*;

public class RecentSignupAdapter extends RecyclerView.Adapter<RecentSignupAdapter.VH> {

    public interface OnRemoveListener {
        void onRemove(Map<String, Object> user);
    }

    private final List<Map<String, Object>> data;
    private final OnRemoveListener listener;

    public RecentSignupAdapter(List<Map<String, Object>> data, OnRemoveListener listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_signup, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Map<String, Object> user = data.get(position);
        String name = String.valueOf(user.getOrDefault("name", "Unknown"));
        String email = String.valueOf(user.getOrDefault("email", ""));

        holder.tvAvatar.setText(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase());
        holder.tvName.setText(name);
        holder.tvEmail.setText(email);
        holder.btnRemove.setOnClickListener(v -> listener.onRemove(user));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvEmail;
        Button btnRemove;

        VH(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvRecentAvatarLetter);
            tvName = itemView.findViewById(R.id.tvRecentUserName);
            tvEmail = itemView.findViewById(R.id.tvRecentUserEmail);
            btnRemove = itemView.findViewById(R.id.btnRemoveRecentSignup);
        }
    }
}