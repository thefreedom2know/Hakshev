package com.example.hakshev;

import android.content.Context;

/** Codes and Hebrew labels for soldier statuses and discipline types. */
public final class Status {
    private Status() {}

    public static final String PRESENT = "PRESENT";
    public static final String SICK = "SICK";
    public static final String DUTY = "DUTY";
    public static final String LEAVE = "LEAVE";
    public static final String PROBLEM = "PROBLEM";
    public static final String[] ALL = {PRESENT, SICK, DUTY, LEAVE, PROBLEM};

    public static final String TYPE_EXIT_HOURS = "EXIT_HOURS";
    public static final String TYPE_EXTRA_GUARD = "EXTRA_GUARD";
    public static final String TYPE_RESTRICTION = "RESTRICTION";
    public static final String TYPE_NOTE_NEG = "NOTE_NEG";
    public static final String TYPE_NOTE_POS = "NOTE_POS";
    public static final String[] TYPES = {
            TYPE_EXIT_HOURS, TYPE_EXTRA_GUARD, TYPE_RESTRICTION, TYPE_NOTE_NEG, TYPE_NOTE_POS};

    /** On site = counted in the headcount. */
    public static boolean onSite(String status) {
        return PRESENT.equals(status) || DUTY.equals(status);
    }

    public static String label(Context c, String status) {
        if (SICK.equals(status)) return c.getString(R.string.status_sick);
        if (DUTY.equals(status)) return c.getString(R.string.status_duty);
        if (LEAVE.equals(status)) return c.getString(R.string.status_leave);
        if (PROBLEM.equals(status)) return c.getString(R.string.status_problem);
        return c.getString(R.string.status_present);
    }

    public static String[] labels(Context c) {
        String[] out = new String[ALL.length];
        for (int i = 0; i < ALL.length; i++) out[i] = label(c, ALL[i]);
        return out;
    }

    public static String typeLabel(Context c, String type) {
        if (TYPE_EXTRA_GUARD.equals(type)) return c.getString(R.string.type_extra_guard);
        if (TYPE_RESTRICTION.equals(type)) return c.getString(R.string.type_restriction);
        if (TYPE_NOTE_NEG.equals(type)) return c.getString(R.string.type_note_neg);
        if (TYPE_NOTE_POS.equals(type)) return c.getString(R.string.type_note_pos);
        return c.getString(R.string.type_exit_hours);
    }

    public static String[] typeLabels(Context c) {
        String[] out = new String[TYPES.length];
        for (int i = 0; i < TYPES.length; i++) out[i] = typeLabel(c, TYPES[i]);
        return out;
    }

    public static int indexOf(String[] arr, String value) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(value)) return i;
        return 0;
    }
}
