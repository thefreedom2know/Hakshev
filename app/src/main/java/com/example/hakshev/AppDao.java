package com.example.hakshev;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AppDao {
    // ---- soldiers ----
    @Insert
    long insertSoldier(Soldier s);

    @Insert
    void insertSoldiers(List<Soldier> list);

    @Update
    void updateSoldier(Soldier s);

    @Delete
    void deleteSoldier(Soldier s);

    @Query("SELECT * FROM soldiers ORDER BY squad, fullName")
    List<Soldier> allSoldiers();

    @Query("SELECT * FROM soldiers WHERE id = :id")
    Soldier soldier(long id);

    @Query("UPDATE soldiers SET waterCount = MAX(0, waterCount + :delta), updatedAt = :now WHERE id = :id")
    void addWater(long id, int delta, long now);

    @Query("UPDATE soldiers SET waterCount = waterCount + :delta, updatedAt = :now WHERE id IN (:ids)")
    void addWaterBulk(List<Long> ids, int delta, long now);

    @Query("UPDATE soldiers SET status = :status, updatedAt = :now WHERE id IN (:ids)")
    void setStatusBulk(List<Long> ids, String status, long now);

    @Query("UPDATE soldiers SET waterCount = 0")
    void resetWater();

    // ---- discipline ----
    @Insert
    void insertDiscipline(DisciplineEntry e);

    @Delete
    void deleteDiscipline(DisciplineEntry e);

    @Query("SELECT * FROM discipline WHERE soldierId = :soldierId ORDER BY createdAt DESC")
    List<DisciplineEntry> disciplineFor(long soldierId);

    @Query("SELECT * FROM discipline ORDER BY createdAt DESC")
    List<DisciplineEntry> allDiscipline();

    @Query("SELECT soldierId, SUM(hours) AS total FROM discipline WHERE type = 'EXIT_HOURS' GROUP BY soldierId")
    List<HoursSum> exitHoursSums();

    // ---- duties ----
    @Insert
    void insertDuty(Duty d);

    @Delete
    void deleteDuty(Duty d);

    @Query("SELECT * FROM duties WHERE endTime >= :since ORDER BY startTime")
    List<Duty> dutiesSince(long since);

    @Query("SELECT * FROM duties WHERE soldierId = :soldierId")
    List<Duty> dutiesFor(long soldierId);

    @Query("SELECT * FROM duties WHERE startTime >= :since")
    List<Duty> dutiesStartedSince(long since);

    // ---- checklists ----
    @Insert
    long insertTemplate(ChecklistTemplate t);

    @Update
    void updateTemplate(ChecklistTemplate t);

    @Delete
    void deleteTemplate(ChecklistTemplate t);

    @Query("SELECT * FROM checklist_templates ORDER BY name")
    List<ChecklistTemplate> templates();

    @Query("SELECT COUNT(*) FROM checklist_templates")
    int templateCount();

    @Insert
    void insertRun(InspectionRun r);

    @Query("SELECT * FROM inspection_runs ORDER BY runAt DESC LIMIT 30")
    List<InspectionRun> recentRuns();

    // ---- hydration log ----
    @Insert
    void insertHydrationLog(HydrationLog l);

    @Query("SELECT * FROM hydration_logs WHERE at >= :since ORDER BY at")
    List<HydrationLog> hydrationLogsSince(long since);
}
