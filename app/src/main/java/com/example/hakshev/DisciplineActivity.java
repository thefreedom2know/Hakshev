package com.example.hakshev;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Discipline (exit hours, punishments) and guard / chore assignments. */
public class DisciplineActivity extends BaseActivity {
    /** Required contiguous rest between two shifts of the same soldier (sleep rule). */
    private static final long REST_MS = 6L * 3600 * 1000;

    private boolean dutiesTab = false;
    private TextAdapter adapter;
    private MaterialButton btnAdd;

    private List<Soldier> soldiers = new ArrayList<>();
    private List<Duty> duties = new ArrayList<>();
    private final List<Long> rowSoldierIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discipline);
        btnAdd = findViewById(R.id.btnAdd);

        adapter = new TextAdapter();
        adapter.setListener(new TextAdapter.Listener() {
            @Override
            public void onClick(int position) {
                if (!dutiesTab && position >= 0 && position < rowSoldierIds.size()) {
                    startActivity(new Intent(DisciplineActivity.this, SoldierActivity.class)
                            .putExtra(SoldierActivity.EXTRA_ID, rowSoldierIds.get(position)));
                }
            }

            @Override
            public void onLongClick(int position) {
                if (dutiesTab) confirmDeleteDuty(position);
            }
        });
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        MaterialButtonToggleGroup toggle = findViewById(R.id.toggle);
        toggle.check(R.id.tabHours);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            dutiesTab = checkedId == R.id.tabDuties;
            Db.IO.execute(this::reload);
        });

        btnAdd.setOnClickListener(v -> {
            if (dutiesTab) addDuty();
            else Dialogs.addDiscipline(this, soldiers, 0, () -> Db.IO.execute(this::reload));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Db.IO.execute(this::reload);
    }

    /** IO thread. */
    private void reload() {
        AppDao dao = Db.dao(this);
        List<Soldier> sl = dao.allSoldiers();
        List<DisciplineEntry> entries = dao.allDiscipline();
        long now = System.currentTimeMillis();
        List<Duty> ds = dao.dutiesSince(now - 12L * 3600 * 1000);
        List<Duty> week = dao.dutiesStartedSince(now - 7L * 86_400_000L);
        runOnUiThread(() -> {
            soldiers = sl;
            duties = ds;
            if (dutiesTab) renderDuties(week);
            else renderHours(entries);
        });
    }

    private Soldier byId(long id) {
        for (Soldier s : soldiers) if (s.id == id) return s;
        return null;
    }

    private void renderHours(List<DisciplineEntry> entries) {
        btnAdd.setText(R.string.add_discipline);
        Map<Long, List<DisciplineEntry>> bySoldier = new HashMap<>();
        for (DisciplineEntry e : entries) {
            List<DisciplineEntry> l = bySoldier.get(e.soldierId);
            if (l == null) {
                l = new ArrayList<>();
                bySoldier.put(e.soldierId, l);
            }
            l.add(e);
        }
        // Soldiers with the most accumulated exit hours first.
        List<Soldier> withEntries = new ArrayList<>();
        for (Soldier s : soldiers) if (bySoldier.containsKey(s.id)) withEntries.add(s);
        withEntries.sort((a, b) -> hoursOf(bySoldier.get(b.id)) - hoursOf(bySoldier.get(a.id)));

        List<String> rows = new ArrayList<>();
        rowSoldierIds.clear();
        for (Soldier s : withEntries) {
            List<DisciplineEntry> l = bySoldier.get(s.id);
            StringBuilder sb = new StringBuilder();
            sb.append(s.fullName).append(" — ")
                    .append(getString(R.string.hours_total, hoursOf(l))).append("\n");
            for (DisciplineEntry e : l) {
                sb.append("• ").append(Status.typeLabel(this, e.type));
                if (e.hours > 0) sb.append(" ").append(e.hours).append(getString(R.string.hours_short));
                if (!e.reason.isEmpty()) sb.append(": ").append(e.reason);
                sb.append(" (").append(e.recordedBy).append(", ").append(Ui.dateTime(e.createdAt)).append(")\n");
            }
            rows.add(sb.toString().trim());
            rowSoldierIds.add(s.id);
        }
        if (rows.isEmpty()) rows.add(getString(R.string.no_discipline));
        adapter.setItems(rows);
    }

    private int hoursOf(List<DisciplineEntry> l) {
        int h = 0;
        for (DisciplineEntry e : l) h += e.hours;
        return h;
    }

    private void renderDuties(List<Duty> week) {
        btnAdd.setText(R.string.add_duty);
        List<String> rows = new ArrayList<>();

        // Fairness: number of shifts per soldier in the last 7 days.
        Map<Long, Integer> counts = new HashMap<>();
        for (Duty d : week) counts.merge(d.soldierId, 1, Integer::sum);
        List<Soldier> sorted = new ArrayList<>(soldiers);
        sorted.sort((a, b) -> {
            int ca = counts.containsKey(a.id) ? counts.get(a.id) : 0;
            int cb = counts.containsKey(b.id) ? counts.get(b.id) : 0;
            return ca - cb;
        });
        StringBuilder fair = new StringBuilder(getString(R.string.fairness_title)).append("\n");
        for (Soldier s : sorted) {
            int c = counts.containsKey(s.id) ? counts.get(s.id) : 0;
            fair.append(s.fullName).append(": ").append(c).append("   ");
        }
        rows.add(fair.toString().trim());

        for (Duty d : duties) {
            Soldier s = byId(d.soldierId);
            rows.add(d.name + " — " + (s == null ? "?" : s.fullName) + "\n"
                    + Ui.dateTime(d.startTime) + " → " + Ui.dateTime(d.endTime));
        }
        if (duties.isEmpty()) rows.add(getString(R.string.no_duties));
        adapter.setItems(rows);
    }

    private void confirmDeleteDuty(int position) {
        int idx = position - 1; // first row is the fairness summary
        if (idx < 0 || idx >= duties.size()) return;
        Duty d = duties.get(idx);
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.confirm_delete_duty)
                .setPositiveButton(R.string.yes, (dlg, w) -> Db.IO.execute(() -> {
                    Db.dao(this).deleteDuty(d);
                    reload();
                }))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void addDuty() {
        if (soldiers.isEmpty()) {
            Ui.toast(this, R.string.no_soldiers);
            return;
        }
        View v = getLayoutInflater().inflate(R.layout.dialog_duty, null);
        Spinner spSoldier = v.findViewById(R.id.spSoldier);
        Spinner spDuty = v.findViewById(R.id.spDuty);
        MaterialButton btnTime = v.findViewById(R.id.btnTime);
        CheckBox cbTomorrow = v.findViewById(R.id.cbTomorrow);
        ViewGroup form = v.findViewById(R.id.formDuty);
        EditText etHours = Ui.addField(this, form, R.string.label_duration_hours, "4", InputType.TYPE_CLASS_NUMBER);

        String[] names = new String[soldiers.size()];
        for (int i = 0; i < names.length; i++) names[i] = soldiers.get(i).fullName;
        Ui.setSpinner(spSoldier, this, names);
        String[] dutyNames = getResources().getStringArray(R.array.duty_names);
        Ui.setSpinner(spDuty, this, dutyNames);

        final int[] hm = {Calendar.getInstance().get(Calendar.HOUR_OF_DAY), 0};
        hm[0] = (hm[0] + 1) % 24;
        btnTime.setText(getString(R.string.start_time, String.format("%02d:%02d", hm[0], hm[1])));
        btnTime.setOnClickListener(b -> new TimePickerDialog(this, (tp, h, m) -> {
            hm[0] = h;
            hm[1] = m;
            btnTime.setText(getString(R.string.start_time, String.format("%02d:%02d", h, m)));
        }, hm[0], hm[1], true).show());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_duty)
                .setView(v)
                .setPositiveButton(R.string.save, (d, w) -> {
                    Soldier s = soldiers.get(spSoldier.getSelectedItemPosition());
                    Calendar start = Calendar.getInstance();
                    start.set(Calendar.HOUR_OF_DAY, hm[0]);
                    start.set(Calendar.MINUTE, hm[1]);
                    start.set(Calendar.SECOND, 0);
                    start.set(Calendar.MILLISECOND, 0);
                    if (cbTomorrow.isChecked()) start.add(Calendar.DAY_OF_YEAR, 1);
                    int hours = Math.max(1, Ui.parseInt(etHours.getText(), 4));

                    Duty duty = new Duty();
                    duty.soldierId = s.id;
                    duty.name = dutyNames[spDuty.getSelectedItemPosition()];
                    duty.startTime = start.getTimeInMillis();
                    duty.endTime = duty.startTime + hours * 3600_000L;
                    checkAndInsert(duty, s);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Warns when the soldier would get back-to-back shifts or less than 6h rest. */
    private void checkAndInsert(Duty duty, Soldier s) {
        Db.IO.execute(() -> {
            boolean conflict = false;
            for (Duty other : Db.dao(this).dutiesFor(s.id)) {
                if (duty.startTime < other.endTime + REST_MS && other.startTime < duty.endTime + REST_MS) {
                    conflict = true;
                    break;
                }
            }
            if (!conflict) {
                Db.dao(this).insertDuty(duty);
                reload();
                return;
            }
            runOnUiThread(() -> new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.rest_warning_title)
                    .setMessage(getString(R.string.rest_warning, s.fullName))
                    .setPositiveButton(R.string.assign_anyway, (d, w) -> Db.IO.execute(() -> {
                        Db.dao(this).insertDuty(duty);
                        reload();
                    }))
                    .setNegativeButton(R.string.cancel, null)
                    .show());
        });
    }
}
