package com.example.hakshev;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** A guard shift or chore assignment. */
@Entity(
        tableName = "duties",
        foreignKeys = @ForeignKey(
                entity = Soldier.class,
                parentColumns = "id",
                childColumns = "soldierId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("soldierId"))
public class Duty {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long soldierId;
    public String name = "";
    public long startTime;
    public long endTime;

    // ---- sync ----
    public String uid = java.util.UUID.randomUUID().toString();
    @ColumnInfo(defaultValue = "0")
    public long updatedAt;
    @ColumnInfo(defaultValue = "0")
    public long syncedAt;
}
