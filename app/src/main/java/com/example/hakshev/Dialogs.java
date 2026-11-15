package com.example.hakshev;

import android.app.Activity;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/** Dialogs used from more than one screen. */
public final class Dialogs {
    private Dialogs() {}

    /**
     * Adds a discipline entry. If {@code fixedSoldierId} &gt; 0 the soldier is preselected
     * and locked. The entry records the commander's name and the time.
     */
    public static void addDiscipline(Activity a, List<Soldier> soldiers, long fixedSoldierId, Runnable onDone) {
        if (soldiers.isEmpty()) {
            Ui.toast(a, R.string.no_soldiers);
            return;
        }
        View v = a.getLayoutInflater().inflate(R.layout.dialog_discipline, null);
        Spinner spSoldier = v.findViewById(R.id.spSoldier);
        Spinner spType = v.findViewById(R.id.spType);
        ViewGroup form = v.findViewById(R.id.formDiscipline);
        EditText etHours = Ui.addField(a, form, R.string.label_hours, "1", InputType.TYPE_CLASS_NUMBER);
        EditText etReason = Ui.addField(a, form, R.string.label_reason, "",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        String[] names = new String[soldiers.size()];
        int preselect = 0;
        for (int i = 0; i < soldiers.size(); i++) {
            names[i] = soldiers.get(i).fullName;
            if (soldiers.get(i).id == fixedSoldierId) preselect = i;
        }
        Ui.setSpinner(spSoldier, a, names);
        spSoldier.setSelection(preselect);
        if (fixedSoldierId > 0) spSoldier.setEnabled(false);
        Ui.setSpinner(spType, a, Status.typeLabels(a));

        new MaterialAlertDialogBuilder(a)
                .setTitle(R.string.add_discipline)
                .setView(v)
                .setPositiveButton(R.string.save, (d, w) -> {
                    Soldier s = soldiers.get(spSoldier.getSelectedItemPosition());
                    DisciplineEntry e = new DisciplineEntry();
                    e.soldierId = s.id;
                    e.type = Status.TYPES[spType.getSelectedItemPosition()];
                    e.hours = Status.TYPE_EXIT_HOURS.equals(e.type)
                            ? Math.max(0, Ui.parseInt(etHours.getText(), 0)) : 0;
                    e.reason = etReason.getText().toString().trim();
                    e.recordedBy = Prefs.commander(a);
                    e.createdAt = System.currentTimeMillis();
                    Db.IO.execute(() -> {
                        Db.dao(a).insertDiscipline(e);
                        a.runOnUiThread(onDone);
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
