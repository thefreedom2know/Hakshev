package com.example.hakshev;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** A commander-defined thing to count (water, exit hours, passed tests, ...). */
@Entity(tableName = "counters")
public class CounterDef {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name = "";
    /** One of the Periods.* codes: how often the count starts again from zero. */
    public String period = Periods.DAY;
    /** Optional goal per period, 0 = none. */
    public int target;
    /** Optional unit label, e.g. "שעות". */
    public String unit = "";
    public long createdAt;

    /**
     * Conversion rule: when a soldier reaches this many in the current period, they are taken
     * off and he gets 1 of the counter {@link #convertToId} instead. 0 = no rule.
     */
    @ColumnInfo(defaultValue = "0")
    public int convertAt;
    @ColumnInfo(defaultValue = "0")
    public long convertToId;

    public static final int KIND_COUNT = 0;
    public static final int KIND_CHECK = 1;
    /** KIND_COUNT = a number per soldier, KIND_CHECK = done / not done (e.g. a test passed). */
    @ColumnInfo(defaultValue = "0")
    public int kind;
    /** 0 = no reason asked, 1 = ask for a reason (can be skipped), 2 = reason is required. */
    @ColumnInfo(defaultValue = "0")
    public int reasonMode;

    // ---- sync ----
    public String uid = java.util.UUID.randomUUID().toString();
    @ColumnInfo(defaultValue = "0")
    public long updatedAt;
    @ColumnInfo(defaultValue = "0")
    public long syncedAt;
}
