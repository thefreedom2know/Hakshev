package com.example.hakshev;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** One drink parade (מסדר שתייה). */
@Entity(tableName = "hydration_logs")
public class HydrationLog {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long at;
    public String note = "";
}
