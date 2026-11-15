package com.example.hakshev;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Dashboard: headcount, water average, exceptions list and quick actions. */
public class MainActivity extends BaseActivity {
    private TextView tvDate, tvHeadcount, tvAvgWater, tvAlertCount, tvAlerts;
    private List<Soldier> soldiers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvDate = findViewById(R.id.tvDate);
        tvHeadcount = findViewById(R.id.tvHeadcount);
        tvAvgWater = findViewById(R.id.tvAvgWater);
        tvAlertCount = findViewById(R.id.tvAlertCount);
        tvAlerts = findViewById(R.id.tvAlerts);

        findViewById(R.id.btnWaterAll).setOnClickListener(v -> confirmWaterAll());
        findViewById(R.id.btnNewInspection).setOnClickListener(v ->
                startActivity(new Intent(this, InspectionActivity.class)
                        .putExtra(InspectionActivity.EXTRA_AUTOSTART, true)));
        findViewById(R.id.btnReport).setOnClickListener(v -> chooseReport());
        findViewById(R.id.btnRoster).setOnClickListener(v -> startActivity(new Intent(this, RosterActivity.class)));
        findViewById(R.id.btnHydration).setOnClickListener(v -> startActivity(new Intent(this, HydrationActivity.class)));
        findViewById(R.id.btnDiscipline).setOnClickListener(v -> startActivity(new Intent(this, DisciplineActivity.class)));
        findViewById(R.id.btnInspections).setOnClickListener(v -> startActivity(new Intent(this, InspectionActivity.class)));
        findViewById(R.id.btnSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        final Context c = getApplicationContext();
        Db.IO.execute(() -> {
            // Auto-wipe after a long period of inactivity (optional, off by default).
            int wipeDays = Prefs.wipeDays(c);
            long last = Prefs.lastActive(c);
            if (wipeDays > 0 && last > 0
                    && System.currentTimeMillis() - last > wipeDays * 86_400_000L) {
                Db.wipeAll(c);
            }
            AppDao dao = Db.dao(c);
            Db.ensureSeed(c);
            // Automatic daily reset of the hydration counters.
            if (!Ui.today().equals(Prefs.lastResetDay(c))) {
                dao.resetWater();
                Prefs.setLastResetDay(c, Ui.today());
            }
            List<Soldier> list = dao.allSoldiers();
            runOnUiThread(() -> {
                soldiers = list;
                render();
            });
        });
    }

    private void render() {
        tvDate.setText(DateFormat.getDateInstance(DateFormat.FULL).format(new Date()));
        tvHeadcount.setText(Alerts.headcount(soldiers) + "/" + soldiers.size());
        tvAvgWater.setText(String.format(Locale.getDefault(), "%.1f", Alerts.averageWater(soldiers)));
        List<String> alerts = Alerts.compute(this, soldiers);
        tvAlertCount.setText(String.valueOf(alerts.size()));
        if (alerts.isEmpty()) {
            tvAlerts.setText(R.string.no_alerts);
        } else {
            StringBuilder sb = new StringBuilder();
            for (String a : alerts) sb.append("• ").append(a).append("\n");
            tvAlerts.setText(sb.toString().trim());
        }
    }

    private void confirmWaterAll() {
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.confirm_water_all)
                .setPositiveButton(R.string.yes, (d, w) -> {
                    List<Long> ids = new ArrayList<>();
                    for (Soldier s : soldiers) if (!Status.LEAVE.equals(s.status)) ids.add(s.id);
                    Db.IO.execute(() -> {
                        Db.dao(this).addWaterBulk(ids, 1, System.currentTimeMillis());
                        runOnUiThread(this::refresh);
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void chooseReport() {
        String[] names = {
                getString(R.string.report_morning), getString(R.string.report_headcount_title),
                getString(R.string.report_hydration_title)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.copy_report)
                .setItems(names, (d, which) -> {
                    String text;
                    if (which == 0) text = ReportBuilder.morning(this, soldiers);
                    else if (which == 1) text = ReportBuilder.headcount(this, soldiers);
                    else text = ReportBuilder.hydration(this, soldiers);
                    showReport(text);
                })
                .show();
    }

    private void showReport(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("report", text));
        Ui.toast(this, R.string.copied);
        new MaterialAlertDialogBuilder(this)
                .setMessage(text)
                .setPositiveButton(R.string.close, null)
                .setNeutralButton(R.string.share, (d, w) -> {
                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType("text/plain");
                    send.putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(send, getString(R.string.share)));
                })
                .show();
    }
}
