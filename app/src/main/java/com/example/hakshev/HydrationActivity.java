package com.example.hakshev;

import android.os.Bundle;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Hydration tracker: daily target, per-soldier progress, drink-parade log. */
public class HydrationActivity extends BaseActivity {
    private HydrationAdapter adapter;
    private TextView tvTarget, tvParades;
    private List<Soldier> soldiers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hydration);
        tvTarget = findViewById(R.id.tvTarget);
        tvParades = findViewById(R.id.tvParades);

        adapter = new HydrationAdapter((s, delta) -> Db.IO.execute(() -> {
            Db.dao(this).addWater(s.id, delta, System.currentTimeMillis());
            reload();
        }));
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        findViewById(R.id.btnTargetMinus).setOnClickListener(v -> {
            Prefs.setWaterTarget(this, Prefs.waterTarget(this) - 1);
            render();
        });
        findViewById(R.id.btnTargetPlus).setOnClickListener(v -> {
            Prefs.setWaterTarget(this, Prefs.waterTarget(this) + 1);
            render();
        });
        findViewById(R.id.btnParade).setOnClickListener(v -> drinkParade());
        findViewById(R.id.btnReset).setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.confirm_reset_day)
                .setPositiveButton(R.string.yes, (d, w) -> Db.IO.execute(() -> {
                    Db.dao(this).resetWater();
                    reload();
                }))
                .setNegativeButton(R.string.cancel, null)
                .show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Db.IO.execute(this::reload);
    }

    /** IO thread. */
    private void reload() {
        List<Soldier> list = Db.dao(this).allSoldiers();
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        List<HydrationLog> logs = Db.dao(this).hydrationLogsSince(midnight.getTimeInMillis());
        runOnUiThread(() -> {
            soldiers = list;
            render();
            if (logs.isEmpty()) {
                tvParades.setText(R.string.no_parades_today);
            } else {
                StringBuilder sb = new StringBuilder(getString(R.string.parades_today)).append(" ");
                for (int i = 0; i < logs.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(Ui.time(logs.get(i).at));
                }
                tvParades.setText(sb);
            }
        });
    }

    private void render() {
        int target = Prefs.waterTarget(this);
        tvTarget.setText(String.valueOf(target));
        adapter.setItems(soldiers, target);
    }

    private void drinkParade() {
        List<Long> ids = new ArrayList<>();
        for (Soldier s : soldiers) if (!Status.LEAVE.equals(s.status)) ids.add(s.id);
        Db.IO.execute(() -> {
            long now = System.currentTimeMillis();
            Db.dao(this).addWaterBulk(ids, 1, now);
            HydrationLog log = new HydrationLog();
            log.at = now;
            Db.dao(this).insertHydrationLog(log);
            reload();
        });
    }
}
