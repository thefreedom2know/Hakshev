package com.example.hakshev;

import android.app.Activity;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

/** Dialogs for creating counters, adding amounts and reading history. */
public final class Dialogs {
    private Dialogs() {}

    private static final int TEXT = InputType.TYPE_CLASS_TEXT;
    private static final int NUMBER = InputType.TYPE_CLASS_NUMBER;
    private static final int SIGNED = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;

    // ---------------------------------------------------------------- counters

    /** Offers suggestions first, then opens the form. {@code onCreated} gets the new id. */
    public static void newCounter(Activity a, LongConsumer onCreated) {
        List<String> names = new ArrayList<>();
        String[] presets = a.getResources().getStringArray(R.array.counter_presets);
        for (String p : presets) names.add(p.split("\\|", -1)[0]);
        names.add(a.getString(R.string.counter_custom));
        new MaterialAlertDialogBuilder(a)
                .setTitle(R.string.counter_suggestions_title)
                .setItems(names.toArray(new String[0]), (d, which) -> {
                    CounterDef base = new CounterDef();
                    if (which < presets.length) {
                        String[] f = presets[which].split("\\|", -1);
                        base.name = f[0];
                        base.period = f[1];
                        base.target = Ui.parseInt(f[2], 0);
                        base.unit = f[3];
                        if (f.length > 4) base.kind = Ui.parseInt(f[4], 0);
                        if (f.length > 5) base.reasonMode = Ui.parseInt(f[5], 0);
                    }
                    counterForm(a, base, true, id -> onCreated.accept(id), null);
                })
                .show();
    }

    /**
     * Form for name, unit, target and period. For an existing counter a delete button is
     * shown and {@code onDeleted} runs afterwards.
     */
    public static void editCounter(Activity a, CounterDef def, Runnable onSaved, Runnable onDeleted) {
        counterForm(a, def, false, id -> onSaved.run(), onDeleted);
    }

    private static void counterForm(Activity a, CounterDef def, boolean isNew,
                                    LongConsumer onSaved, Runnable onDeleted) {
        if (def.convertToId == 0) {
            showCounterForm(a, def, "", isNew, onSaved, onDeleted);
            return;
        }
        // Look up the name of the counter this one converts into.
        Db.IO.execute(() -> {
            CounterDef target = Db.dao(a).counter(def.convertToId);
            String name = target == null ? "" : target.name;
            a.runOnUiThread(() -> showCounterForm(a, def, name, isNew, onSaved, onDeleted));
        });
    }

    private static void showCounterForm(Activity a, CounterDef def, String convertName, boolean isNew,
                                        LongConsumer onSaved, Runnable onDeleted) {
        View v = a.getLayoutInflater().inflate(R.layout.dialog_counter, null);
        ViewGroup form = v.findViewById(R.id.form);
        ViewGroup formConvert = v.findViewById(R.id.formConvert);
        CheckBox cbAll = v.findViewById(R.id.cbAll);
        cbAll.setVisibility(isNew ? View.VISIBLE : View.GONE);
        CheckBox cbDefault = v.findViewById(R.id.cbDefault);
        cbDefault.setChecked(def.forAll);
        EditText etConvertAt = Ui.addField(a, formConvert, R.string.label_convert_at,
                def.convertAt > 0 ? String.valueOf(def.convertAt) : "", NUMBER);
        EditText etConvertTo = Ui.addField(a, formConvert, R.string.label_convert_to, convertName, TEXT);
        Spinner spPeriod = v.findViewById(R.id.spPeriod);
        EditText etName = Ui.addField(a, form, R.string.label_counter_name, def.name, TEXT);
        EditText etUnit = Ui.addField(a, form, R.string.label_counter_unit, def.unit, TEXT);
        EditText etTarget = Ui.addField(a, form, R.string.label_counter_target,
                def.target > 0 ? String.valueOf(def.target) : "", NUMBER);
        Ui.setSpinner(spPeriod, a, Periods.labels(a));
        spPeriod.setSelection(Status.indexOf(Periods.ALL, def.period));
        Spinner spKind = v.findViewById(R.id.spKind);
        Ui.setSpinner(spKind, a, new String[]{a.getString(R.string.kind_count), a.getString(R.string.kind_check)});
        spKind.setSelection(Math.max(0, Math.min(1, def.kind)));
        Spinner spReason = v.findViewById(R.id.spReason);
        Ui.setSpinner(spReason, a, new String[]{a.getString(R.string.reason_none),
                a.getString(R.string.reason_optional), a.getString(R.string.reason_required)});
        spReason.setSelection(Math.max(0, Math.min(2, def.reasonMode)));
        View targetBox = Ui.fieldBox(etTarget);
        View unitBox = Ui.fieldBox(etUnit);
        spKind.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int vis = position == CounterDef.KIND_CHECK ? View.GONE : View.VISIBLE;
                targetBox.setVisibility(vis);
                unitBox.setVisibility(vis);
                formConvert.setVisibility(vis);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(a)
                .setTitle(isNew ? R.string.new_counter : R.string.edit_counter)
                .setView(v)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Ui.toast(a, R.string.name_required);
                        return;
                    }
                    def.name = name;
                    def.unit = etUnit.getText().toString().trim();
                    def.target = Math.max(0, Ui.parseInt(etTarget.getText(), 0));
                    def.period = Periods.ALL[Math.max(0, spPeriod.getSelectedItemPosition())];
                    def.forAll = cbDefault.isChecked();
                    def.kind = Math.max(0, spKind.getSelectedItemPosition());
                    def.reasonMode = Math.max(0, spReason.getSelectedItemPosition());
                    if (def.kind == CounterDef.KIND_CHECK) {
                        def.target = 0;
                        def.unit = "";
                    }
                    if (def.createdAt == 0) def.createdAt = System.currentTimeMillis();
                    int convertAt = def.kind == CounterDef.KIND_CHECK
                            ? 0 : Math.max(0, Ui.parseInt(etConvertAt.getText(), 0));
                    String convertTo = etConvertTo.getText().toString().trim();
                    boolean everyone = cbAll.isChecked();
                    Db.IO.execute(() -> {
                        AppDao dao = Db.dao(a);
                        // Conversion: reuse the counter with that name, or create it.
                        def.convertAt = 0;
                        def.convertToId = 0;
                        if (convertAt > 0 && !convertTo.isEmpty() && !convertTo.equals(def.name)) {
                            CounterDef target = dao.counterByName(convertTo);
                            if (target == null) {
                                target = new CounterDef();
                                target.name = convertTo;
                                target.period = Periods.TOTAL;
                                target.createdAt = System.currentTimeMillis();
                                target.updatedAt = target.createdAt;
                                target.id = dao.insertCounter(target);
                            }
                            def.convertAt = convertAt;
                            def.convertToId = target.id;
                        }
                        def.updatedAt = System.currentTimeMillis();
                        long id = def.id;
                        if (isNew) id = dao.insertCounter(def);
                        else dao.updateCounter(def);
                        if (isNew && everyone) {
                            List<CounterMember> all = new ArrayList<>();
                            for (Soldier s : dao.allSoldiers()) all.add(new CounterMember(id, s.id));
                            Track.addMembers(dao, all);
                        }
                        Counters.ensureDefaults(a);
                        final long finalId = id;
                        a.runOnUiThread(() -> onSaved.accept(finalId));
                    });
                })
                .setNegativeButton(R.string.cancel, null);
        if (!isNew && onDeleted != null) {
            b.setNeutralButton(R.string.delete, (d, w) -> new MaterialAlertDialogBuilder(a)
                    .setMessage(a.getString(R.string.confirm_delete_counter, def.name))
                    .setPositiveButton(R.string.yes, (d2, w2) -> Db.IO.execute(() -> {
                        Track.counterDeleted(Db.dao(a), def);
                        Db.dao(a).deleteCounter(def);
                        a.runOnUiThread(onDeleted);
                    }))
                    .setNegativeButton(R.string.cancel, null)
                    .show());
        }
        b.show();
    }

    // ----------------------------------------------------------------- entries

    /** Asks for a reason when the counter wants one (mode 1 = optional, 2 = required). */
    private static void askNote(Activity a, String title, int mode, Consumer<String> onNote) {
        if (mode == 0) {
            onNote.accept("");
            return;
        }
        View v = a.getLayoutInflater().inflate(R.layout.dialog_form, null);
        ViewGroup form = v.findViewById(R.id.form);
        EditText et = Ui.addField(a, form, mode == 2 ? R.string.label_reason_required : R.string.label_reason,
                "", TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        AlertDialog dlg = new MaterialAlertDialogBuilder(a)
                .setTitle(title)
                .setView(v)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String text = et.getText().toString().trim();
            if (mode == 2 && text.isEmpty()) {
                et.setError(a.getString(R.string.reason_required_msg));
                return;
            }
            dlg.dismiss();
            onNote.accept(text);
        }));
        dlg.show();
    }

    /** +1 / -1 buttons: saves at once, or first asks for the reason when the counter wants one. */
    public static void quickDelta(Activity a, CounterDef def, Soldier s, int delta, Runnable afterOnIo) {
        askNote(a, def.name + " — " + s.fullName, def.reasonMode, note -> {
            CounterEntry e = entry(a, def.id, s.id, delta);
            e.note = note;
            Counters.addEntries(a, java.util.Collections.singletonList(e), afterOnIo);
        });
    }

    /** Tap on a row: a custom amount with a reason, or done / not done for a done-list. */
    public static void addAmount(Activity a, CounterDef def, Soldier s, Runnable onDone) {
        if (def.kind == CounterDef.KIND_CHECK) {
            toggleDone(a, def, s, onDone);
            return;
        }
        View v = a.getLayoutInflater().inflate(R.layout.dialog_form, null);
        ViewGroup form = v.findViewById(R.id.form);
        EditText etAmount = Ui.addField(a, form, R.string.label_amount, "1", SIGNED);
        EditText etNote = Ui.addField(a, form,
                def.reasonMode == 2 ? R.string.label_reason_required : R.string.label_reason, "",
                TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        AlertDialog dlg = new MaterialAlertDialogBuilder(a)
                .setTitle(def.name + " — " + s.fullName)
                .setView(v)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dlg.setOnShowListener(x -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            int amount = Ui.parseInt(etAmount.getText(), 0);
            if (amount == 0) return;
            String note = etNote.getText().toString().trim();
            if (def.reasonMode == 2 && note.isEmpty()) {
                etNote.setError(a.getString(R.string.reason_required_msg));
                return;
            }
            dlg.dismiss();
            CounterEntry e = entry(a, def.id, s.id, amount);
            e.note = note;
            Counters.addEntries(a, java.util.Collections.singletonList(e),
                    () -> a.runOnUiThread(onDone));
        }));
        dlg.show();
    }

    /** Done-list: mark as done (with optional details such as a grade), or undo the mark. */
    private static void toggleDone(Activity a, CounterDef def, Soldier s, Runnable onDone) {
        Db.IO.execute(() -> {
            long[] r = Periods.range(def.period, def.createdAt, System.currentTimeMillis(), 0);
            int value = Db.dao(a).total(def.id, s.id, r[0], r[1]);
            a.runOnUiThread(() -> {
                if (value <= 0) {
                    askNote(a, def.name + " — " + s.fullName, Math.max(1, def.reasonMode), note -> {
                        CounterEntry e = entry(a, def.id, s.id, 1);
                        e.note = note;
                        Counters.addEntries(a, java.util.Collections.singletonList(e),
                                () -> a.runOnUiThread(onDone));
                    });
                } else {
                    new MaterialAlertDialogBuilder(a)
                            .setMessage(a.getString(R.string.confirm_unmark, s.fullName))
                            .setPositiveButton(R.string.yes, (d, w) -> {
                                CounterEntry e = entry(a, def.id, s.id, -value);
                                e.note = a.getString(R.string.note_unmarked);
                                Counters.addEntries(a, java.util.Collections.singletonList(e),
                                        () -> a.runOnUiThread(onDone));
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                }
            });
        });
    }

    public static CounterEntry entry(Activity a, long counterId, long soldierId, int delta) {
        CounterEntry e = new CounterEntry();
        e.counterId = counterId;
        e.soldierId = soldierId;
        e.delta = delta;
        e.recordedBy = Prefs.commander(a);
        e.createdAt = System.currentTimeMillis();
        return e;
    }

    /** History of one soldier in one counter. Tapping an entry offers to delete it. */
    public static void history(Activity a, CounterDef def, Soldier s, Runnable onChanged) {
        Db.IO.execute(() -> {
            List<CounterEntry> entries = Db.dao(a).entries(def.id, s.id);
            a.runOnUiThread(() -> {
                if (entries.isEmpty()) {
                    Ui.toast(a, R.string.no_history);
                    return;
                }
                String[] rows = new String[entries.size()];
                for (int i = 0; i < rows.length; i++) {
                    CounterEntry e = entries.get(i);
                    StringBuilder sb = new StringBuilder();
                    sb.append(e.delta > 0 ? "+" : "").append(e.delta);
                    if (!e.note.isEmpty()) sb.append("  ").append(e.note);
                    sb.append("\n").append(e.recordedBy).append(" · ").append(Ui.dateTime(e.createdAt));
                    rows[i] = sb.toString();
                }
                new MaterialAlertDialogBuilder(a)
                        .setTitle(def.name + " — " + s.fullName)
                        .setItems(rows, (d, which) -> new MaterialAlertDialogBuilder(a)
                                .setMessage(R.string.confirm_delete_entry)
                                .setPositiveButton(R.string.yes, (d2, w2) -> Db.IO.execute(() -> {
                                    Track.entryDeleted(Db.dao(a), entries.get(which));
                                    Db.dao(a).deleteEntry(entries.get(which));
                                    a.runOnUiThread(onChanged);
                                }))
                                .setNegativeButton(R.string.cancel, null)
                                .show())
                        .setPositiveButton(R.string.close, null)
                        .setNeutralButton(R.string.remove_from_counter, (d, w) -> Db.IO.execute(() -> {
                            if (def.forAll) {
                                a.runOnUiThread(() -> Ui.toast(a, R.string.cannot_remove_default));
                                return;
                            }
                            Track.memberRemoved(Db.dao(a), def.id, s.id);
                            Db.dao(a).removeMember(def.id, s.id);
                            a.runOnUiThread(onChanged);
                        }))
                        .show();
            });
        });
    }
}
