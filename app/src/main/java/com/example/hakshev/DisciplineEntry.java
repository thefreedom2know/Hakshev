package com.example.hakshev;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Punishment / discipline record, including who entered it and when. */
@Entity(
        tableName = "discipline",
        foreignKeys = @ForeignKey(
                entity = Soldier.class,
                parentColumns = "id",
                childColumns = "soldierId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("soldierId"))
public class DisciplineEntry {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long soldierId;
    /** One of the Status.TYPE_* codes. */
    public String type = Status.TYPE_EXIT_HOURS;
    public int hours;
    public String reason = "";
    public String recordedBy = "";
    public long createdAt;
}
