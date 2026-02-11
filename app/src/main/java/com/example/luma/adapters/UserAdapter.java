package com.example.luma.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luma.R;
import com.example.luma.models.User;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private final List<User> userList;
    private final OnUserClickListener onUserClickListener;
    public UserAdapter(@Nullable final OnUserClickListener onUserClickListener) {
        this.userList = new ArrayList<>();
        this.onUserClickListener = onUserClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        if (user == null) return;

        // שם ואימייל
        holder.tvName.setText(user.getFirstName());
        holder.tvEmail.setText(user.getEmail());

        // ראשי תיבות
        String initials = "";
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            initials = String.valueOf(user.getFirstName().charAt(0));
        }
        holder.tvInitials.setText(initials.toUpperCase());

        // הצגת Chip אם המשתמש אדמין
        if (Objects.requireNonNull(onUserClickListener).showAdminChip(user)) {
            holder.chipRole.setVisibility(View.VISIBLE);
            holder.chipRole.setText("Admin");
        } else {
            holder.chipRole.setVisibility(View.GONE);
        }

        // לחיצה רגילה
        holder.itemView.setOnClickListener(v -> {
            onUserClickListener.onUserClick(user);
        });

        // לחיצה ארוכה
        holder.itemView.setOnLongClickListener(v -> {
            onUserClickListener.onLongUserClick(user);
            return true;
        });
        holder.tvName.setOnClickListener(v -> {

        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void setUserList(List<User> users) {
        userList.clear();
        userList.addAll(users);
        notifyDataSetChanged();
    }

    // =======================
    // ניהול רשימה
    // =======================

    public void addUser(User user) {
        userList.add(user);
        notifyItemInserted(userList.size() - 1);
    }

    /**
     * ⚠️ תיקון קריטי:
     * לא משתמשים ב-indexOf
     * מחפשים משתמש לפי ID
     */
    public void updateUser(User updatedUser) {
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getId().equals(updatedUser.getId())) {
                userList.set(i, updatedUser);
                notifyItemChanged(i);
                return;
            }
        }
    }

    /**
     * ⚠️ תיקון קריטי:
     * מחיקה לפי ID ולא לפי אובייקט
     */
    public void removeUser(User user) {
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getId().equals(user.getId())) {
                userList.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    // =======================
    // Listener ללחיצות
    // =======================
    public interface OnUserClickListener {
        void onUserClick(User user);

        void onLongUserClick(User user);

        boolean showAdminChip(User user);

        boolean showRemoveAdminBtn(User user);

        boolean showMakeAdminBtn(User user);
    }

    // =======================
    // ViewHolder
    // =======================
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvEmail;
        final TextView tvInitials;
        final Chip chipRole;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_item_user_name);
            tvEmail = itemView.findViewById(R.id.tv_item_user_email);
            tvInitials = itemView.findViewById(R.id.tv_user_initials);
            chipRole = itemView.findViewById(R.id.chip_user_role);
        }
    }
}
