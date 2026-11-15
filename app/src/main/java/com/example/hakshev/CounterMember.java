package com.example.hakshev;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;

/** A soldier taking part in a counter. */
@Entity(
        tableName = "counter_members",
        primaryKeys = {"counterId", "soldierId"},
        foreignKeys = {
                @ForeignKey(entity = CounterDef.class, parentColumns = "id",
                        childColumns = "counterId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Soldier.class, parentColumns = "id",
                        childColumns = "soldierId", onDelete = ForeignKey.CASCADE)},
        indices = {@Index("soldierId")})
public class CounterMember {
    public long counterId;
    public long soldierId;

    // ---- sync ----
    @ColumnInfo(defaultValue = "0")
    public long updatedAt;
    @ColumnInfo(defaultValue = "0")
    public long syncedAt;

    public CounterMember() {}

    @Ignore
    public CounterMember(long counterId, long soldierId) {
        this.counterId = counterId;
        this.soldierId = soldierId;
        this.updatedAt = System.currentTimeMillis();
    }
}
