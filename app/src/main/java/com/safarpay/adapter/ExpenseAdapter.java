package com.safarpay.adapter;

import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import com.safarpay.R;
import com.safarpay.data.local.entity.Expense;
import java.util.*;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.VH> {

    public interface OnItemClickListener  { void onItemClick(Expense e); }
    public interface OnItemSwipeListener { void onItemSwiped(Expense e, int pos); }

    private List<Expense> expenses = new ArrayList<>();
    private OnItemClickListener clickListener;
    private OnItemSwipeListener swipeListener;

    public void setExpenses(List<Expense> list) {
        this.expenses = (list == null) ? new ArrayList<>() : list;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener l)  { clickListener = l; }
    public void setOnItemSwipeListener(OnItemSwipeListener l) { swipeListener = l; }

    public Expense getItem(int pos) { return expenses.get(pos); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Expense e = expenses.get(position);
        holder.tvCategory.setText(e.category);
        holder.tvNote.setText(e.note != null && !e.note.isEmpty() ? e.note : "(no note)");
        holder.tvDate.setText(e.date);
        holder.tvAmount.setText(String.format(Locale.getDefault(), "%.2f %s", e.amount, e.currency));
        holder.tvAmountPKR.setText(String.format(Locale.getDefault(), "PKR %.0f", e.amountPKR));
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onItemClick(e);
        });
    }

    @Override public int getItemCount() { return expenses.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCategory, tvNote, tvDate, tvAmount, tvAmountPKR;
        VH(View v) {
            super(v);
            tvCategory  = v.findViewById(R.id.tvCategory);
            tvNote      = v.findViewById(R.id.tvNote);
            tvDate      = v.findViewById(R.id.tvDate);
            tvAmount    = v.findViewById(R.id.tvAmount);
            tvAmountPKR = v.findViewById(R.id.tvAmountPKR);
        }
    }

    /** Attach swipe-to-delete to a RecyclerView */
    public void attachSwipe(RecyclerView rv) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override public boolean onMove(@NonNull RecyclerView rv2,
                @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getAdapterPosition();
                Expense e = expenses.get(pos);
                if (swipeListener != null) swipeListener.onItemSwiped(e, pos);
            }
        }).attachToRecyclerView(rv);
    }
}
