package com.example.luma.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.luma.R;
import com.example.luma.models.Psychologist;

import java.util.ArrayList;
import java.util.List;

public class PsychologistAdapter extends RecyclerView.Adapter<PsychologistAdapter.ViewHolder> {

    // =======================
    // Listener ללחיצות
    // =======================
    public interface OnClickListener {
        void onClick(Psychologist psychologist);
        void onLongClick(Psychologist psychologist);

    }

    private final List<Psychologist> psychologistList;
    private final OnClickListener onClickListener;

    public PsychologistAdapter(@Nullable final OnClickListener onClickListener) {
        this.psychologistList = new ArrayList<>();
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_psychologist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Psychologist psychologist = psychologistList.get(position);
        if (psychologist == null) return;

        // שם ואימייל
        holder.tvName.setText(psychologist.getName());
        holder.tvEmail.setText(psychologist.getEmail());
        holder.tvCity.setText(psychologist.getCity());
        holder.tvSessionPrice.setText(String.valueOf(psychologist.getSessionPrice()));


        // לחיצה רגילה
        holder.itemView.setOnClickListener(v -> {
            if (onClickListener != null) {
                onClickListener.onClick(psychologist);
            }
        });

        // לחיצה ארוכה
        holder.itemView.setOnLongClickListener(v -> {
            if (onClickListener != null) {
                onClickListener.onLongClick(psychologist);
            }
            return true;
        });
        holder.tvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return psychologistList.size();
    }

    // =======================
    // ניהול רשימה
    // =======================

    public void setList(List<Psychologist> psychologist) {
        psychologistList.clear();
        psychologistList.addAll(psychologist);
        notifyDataSetChanged();
    }

    public void add(Psychologist psychologist) {
        psychologistList.add(psychologist);
        notifyItemInserted(psychologistList.size() - 1);
    }

    /**
     * מחפשים משתמש לפי ID
     */
    public void update(Psychologist updatedPsychologist) {
        for (int i = 0; i < psychologistList.size(); i++) {
            if (psychologistList.get(i).getId().equals(updatedPsychologist.getId())) {
                psychologistList.set(i, updatedPsychologist);
                notifyItemChanged(i);
                return;
            }
        }
    }

    /**
     * ⚠️ תיקון קריטי:
     * מחיקה לפי ID ולא לפי אובייקט
     */
    public void remove(Psychologist psychologist) {
        for (int i = 0; i < psychologistList.size(); i++) {
            if (psychologistList.get(i).getId().equals(psychologist.getId())) {
                psychologistList.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    // =======================
    // ViewHolder
    // =======================
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvCity, tvSessionPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_item_psychologist_name);
            tvEmail = itemView.findViewById(R.id.tv_item_psychologist_email);
            tvCity = itemView.findViewById(R.id.tv_item_psychologist_city);
            tvSessionPrice = itemView.findViewById(R.id.tv_item_psychologist_sessionPrice);
        }
    }
}
