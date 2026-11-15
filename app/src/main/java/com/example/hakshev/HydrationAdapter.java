package com.example.hakshev;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/** Hydration rows with a green / yellow / red progress bar per soldier. */
public class HydrationAdapter extends RecyclerView.Adapter<HydrationAdapter.VH> {
    public interface Listener {
        void onWater(Soldier s, int delta);
    }

    private final List<Soldier> items = new ArrayList<>();
    private final Listener listener;
    private int target = 4;

    public HydrationAdapter(Listener l) {
        this.listener = l;
    }

    public void setItems(List<Soldier> list, int target) {
        this.target = target;
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hydration, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Soldier s = items.get(position);
        h.name.setText(s.fullName);
        h.count.setText(s.waterCount + "/" + target);
        h.progress.setMax(target);
        h.progress.setProgress(Math.min(s.waterCount, target));

        int expected = Alerts.expectedWater(target);
        int colorRes;
        if (s.waterCount >= target || s.waterCount >= expected) {
            colorRes = R.color.hk_ok;
        } else if (expected - s.waterCount >= 2) {
            colorRes = R.color.hk_alert;
        } else {
            colorRes = R.color.hk_warn;
        }
        h.progress.setProgressTintList(
                ColorStateList.valueOf(ContextCompat.getColor(h.itemView.getContext(), colorRes)));

        h.minus.setOnClickListener(v -> listener.onWater(s, -1));
        h.plus.setOnClickListener(v -> listener.onWater(s, 1));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, count;
        final ProgressBar progress;
        final MaterialButton minus, plus;

        VH(View v) {
            super(v);
            name = v.findViewById(R.id.tvName);
            count = v.findViewById(R.id.tvCount);
            progress = v.findViewById(R.id.progress);
            minus = v.findViewById(R.id.btnMinus);
            plus = v.findViewById(R.id.btnPlus);
        }
    }
}
