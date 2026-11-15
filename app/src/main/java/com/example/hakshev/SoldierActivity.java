package com.example.hakshev;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Soldier profile: personal, medical, equipment and discipline history. */
public class SoldierActivity extends BaseActivity {
    public static final String EXTRA_ID = "soldier_id";

    private static final int TEXT = InputType.TYPE_CLASS_TEXT;
    private static final int PHONE = InputType.TYPE_CLASS_PHONE;
    private static final int MULTI = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE;

    private long soldierId;
    private Soldier soldier = new Soldier();
    private long expiry;

    private EditText etName, etPersonal, etPhone, etParentPhone, etShoe, etUniform;
    private EditText etAllergies, etExemptions, etMeds, etWeapon, etEquipment, etNotes;
    private Spinner spSquad, spStatus;
    private MaterialButton btnExpiry;
    private TextView tvBanner, tvHoursTotal, tvTitle;
    private ViewGroup disciplineList;
    private List<String> squads = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soldier);
        soldierId = getIntent().getLongExtra(EXTRA_ID, 0);

        tvTitle = findViewById(R.id.tvTitle);
        tvBanner = findViewById(R.id.tvAlertBanner);
        tvHoursTotal = findViewById(R.id.tvHoursTotal);
        disciplineList = findViewById(R.id.disciplineList);
        spSquad = findViewById(R.id.spSquad);
        spStatus = findViewById(R.id.spStatus);
        btnExpiry = findViewById(R.id.btnExpiry);

        ViewGroup personal = findViewById(R.id.formPersonal);
        etName = Ui.addField(this, personal, R.string.label_name, "", TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etPersonal = Ui.addField(this, personal, R.string.label_personal_number, "", InputType.TYPE_CLASS_NUMBER);
        etPhone = Ui.addField(this, personal, R.string.label_phone, "", PHONE);
        etParentPhone = Ui.addField(this, personal, R.string.label_parent_phone, "", PHONE);
        etShoe = Ui.addField(this, personal, R.string.label_shoe, "", TEXT);
        etUniform = Ui.addField(this, personal, R.string.label_uniform, "", TEXT);

        ViewGroup medical = findViewById(R.id.formMedical);
        etAllergies = Ui.addField(this, medical, R.string.label_allergies, "", MULTI);
        etExemptions = Ui.addField(this, medical, R.string.label_exemptions, "", MULTI);
        etMeds = Ui.addField(this, medical, R.string.label_meds, "", TEXT);

        ViewGroup equipment = findViewById(R.id.formEquipment);
        etWeapon = Ui.addField(this, equipment, R.string.label_weapon, "", TEXT);
        etEquipment = Ui.addField(this, equipment, R.string.label_equipment, "", MULTI);
        etNotes = Ui.addField(this, equipment, R.string.label_notes, "", MULTI);

        squads = new ArrayList<>(Prefs.squads(this));
        Ui.setSpinner(spStatus, this, Status.labels(this));

        btnExpiry.setOnClickListener(v -> pickExpiry());
        btnExpiry.setOnLongClickListener(v -> {
            expiry = 0;
            updateExpiryButton();
            return true;
        });
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDelete());
        findViewById(R.id.btnAddDiscipline).setOnClickListener(v -> addDiscipline());

        if (soldierId == 0) {
            tvTitle.setText(R.string.new_soldier);
            findViewById(R.id.btnDelete).setVisibility(View.GONE);
            findViewById(R.id.btnAddDiscipline).setVisibility(View.GONE);
            Ui.setSpinner(spSquad, this, squads);
            updateExpiryButton();
        } else {
            Db.IO.execute(() -> {
                Soldier s = Db.dao(this).soldier(soldierId);
                runOnUiThread(() -> {
                    if (s == null) {
                        finish();
                        return;
                    }
                    soldier = s;
                    populate();
                });
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (soldierId != 0) loadDiscipline();
    }

    private void populate() {
        tvTitle.setText(soldier.fullName);
        etName.setText(soldier.fullName);
        etPersonal.setText(soldier.personalNumber);
        etPhone.setText(soldier.phone);
        etParentPhone.setText(soldier.parentPhone);
        etShoe.setText(soldier.shoeSize);
        etUniform.setText(soldier.uniformSize);
        etAllergies.setText(soldier.allergies);
        etExemptions.setText(soldier.exemptions);
        etMeds.setText(soldier.medTimes);
        etWeapon.setText(soldier.weaponNumber);
        etEquipment.setText(soldier.equipment);
        etNotes.setText(soldier.notes);
        expiry = soldier.exemptionExpiry;

        if (!soldier.squad.isEmpty() && !squads.contains(soldier.squad)) squads.add(soldier.squad);
        Ui.setSpinner(spSquad, this, squads);
        int idx = squads.indexOf(soldier.squad);
        if (idx >= 0) spSquad.setSelection(idx);
        spStatus.setSelection(Status.indexOf(Status.ALL, soldier.status));

        updateExpiryButton();
        updateBanner();
    }

    private void updateBanner() {
        String allergies = etAllergies.getText().toString().trim();
        if (allergies.isEmpty()) {
            tvBanner.setVisibility(View.GONE);
        } else {
            tvBanner.setText(getString(R.string.allergy_banner, allergies));
            tvBanner.setVisibility(View.VISIBLE);
        }
    }

    private void updateExpiryButton() {
        btnExpiry.setText(expiry > 0
                ? getString(R.string.exemption_until, Ui.date(expiry))
                : getString(R.string.set_exemption_expiry));
    }

    private void pickExpiry() {
        Calendar cal = Calendar.getInstance();
        if (expiry > 0) cal.setTimeInMillis(expiry);
        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(y, m, d, 23, 59, 0);
            expiry = picked.getTimeInMillis();
            updateExpiryButton();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadDiscipline() {
        Db.IO.execute(() -> {
            List<DisciplineEntry> entries = Db.dao(this).disciplineFor(soldierId);
            runOnUiThread(() -> renderDiscipline(entries));
        });
    }

    private void renderDiscipline(List<DisciplineEntry> entries) {
        disciplineList.removeAllViews();
        int hours = 0;
        for (DisciplineEntry e : entries) {
            hours += e.hours;
            StringBuilder sb = new StringBuilder(Status.typeLabel(this, e.type));
            if (e.hours > 0) sb.append(" (").append(e.hours).append(" ").append(getString(R.string.hours_short)).append(")");
            if (!e.reason.isEmpty()) sb.append(": ").append(e.reason);
            sb.append("\n").append(e.recordedBy).append(" · ").append(Ui.dateTime(e.createdAt));

            TextView tv = new TextView(this);
            tv.setText(sb);
            tv.setTextSize(16);
            tv.setMinHeight((int) (56 * getResources().getDisplayMetrics().density));
            tv.setPadding(8, 12, 8, 12);
            tv.setTextColor(tvHoursTotal.getTextColors());
            tv.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.confirm_delete_entry)
                    .setPositiveButton(R.string.yes, (d, w) -> Db.IO.execute(() -> {
                        Db.dao(this).deleteDiscipline(e);
                        loadDiscipline();
                    }))
                    .setNegativeButton(R.string.cancel, null)
                    .show());
            disciplineList.addView(tv);
        }
        tvHoursTotal.setText(getString(R.string.hours_total, hours));
    }

    private void addDiscipline() {
        List<Soldier> one = new ArrayList<>();
        one.add(soldier);
        Dialogs.addDiscipline(this, one, soldierId, this::loadDiscipline);
    }

    private void save() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError(getString(R.string.name_required));
            return;
        }
        soldier.fullName = name;
        soldier.personalNumber = etPersonal.getText().toString().trim();
        soldier.phone = etPhone.getText().toString().trim();
        soldier.parentPhone = etParentPhone.getText().toString().trim();
        soldier.shoeSize = etShoe.getText().toString().trim();
        soldier.uniformSize = etUniform.getText().toString().trim();
        soldier.allergies = etAllergies.getText().toString().trim();
        soldier.exemptions = etExemptions.getText().toString().trim();
        soldier.exemptionExpiry = expiry;
        soldier.medTimes = etMeds.getText().toString().trim();
        soldier.weaponNumber = etWeapon.getText().toString().trim();
        soldier.equipment = etEquipment.getText().toString().trim();
        soldier.notes = etNotes.getText().toString().trim();
        int sq = spSquad.getSelectedItemPosition();
        soldier.squad = sq >= 0 && sq < squads.size() ? squads.get(sq) : "";
        soldier.status = Status.ALL[Math.max(0, spStatus.getSelectedItemPosition())];
        soldier.updatedAt = System.currentTimeMillis();

        Db.IO.execute(() -> {
            if (soldierId == 0) Db.dao(this).insertSoldier(soldier);
            else Db.dao(this).updateSoldier(soldier);
            runOnUiThread(this::finish);
        });
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setMessage(getString(R.string.confirm_delete_soldier, soldier.fullName))
                .setPositiveButton(R.string.yes, (d, w) -> Db.IO.execute(() -> {
                    Db.dao(this).deleteSoldier(soldier);
                    runOnUiThread(this::finish);
                }))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
