package com.example.hakshev;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Remembers that a record was deleted here, so the deletion can be sent to the other
 * commanders. Removed once it has been pushed.
 */
@Entity(tableName = "tombstones")
public class Tombstone {
    @PrimaryKey
    @NonNull
    public String key = "";

    @NonNull
    public String type = "";
    @NonNull
    public String uid = "";
    public long deletedAt;

    public Tombstone() {}

    @Ignore
    public Tombstone(String type, String uid) {
        this.type = type;
        this.uid = uid;
        this.key = type + "_" + uid;
        this.deletedAt = System.currentTimeMillis();
    }
}
