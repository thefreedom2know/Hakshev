package com.example.hakshev;

import java.util.List;

/** Bookkeeping for sync: records deletions and re-additions. Call on {@link Db#IO}. */
public final class Track {
    private Track() {}

    public static String memberUid(String counterUid, String soldierUid) {
        return counterUid + "_" + soldierUid;
    }

    public static void soldierDeleted(AppDao dao, Soldier s) {
        if (s.uid != null) dao.insertTombstone(new Tombstone("soldier", s.uid));
    }

    public static void counterDeleted(AppDao dao, CounterDef c) {
        if (c.uid != null) dao.insertTombstone(new Tombstone("counter", c.uid));
    }

    public static void entryDeleted(AppDao dao, CounterEntry e) {
        if (e.uid != null) dao.insertTombstone(new Tombstone("entry", e.uid));
    }

    public static void dutyDeleted(AppDao dao, Duty d) {
        if (d.uid != null) dao.insertTombstone(new Tombstone("duty", d.uid));
    }

    public static void memberRemoved(AppDao dao, long counterId, long soldierId) {
        CounterDef c = dao.counter(counterId);
        Soldier s = dao.soldier(soldierId);
        if (c != null && s != null && c.uid != null && s.uid != null) {
            dao.insertTombstone(new Tombstone("member", memberUid(c.uid, s.uid)));
        }
    }

    /** Adds members; a member that was removed earlier is re-added (the tombstone is dropped). */
    public static void addMembers(AppDao dao, List<CounterMember> list) {
        long now = System.currentTimeMillis();
        for (CounterMember m : list) {
            CounterMember existing = dao.member(m.counterId, m.soldierId);
            if (existing != null) continue;
            m.updatedAt = now;
            CounterDef c = dao.counter(m.counterId);
            Soldier s = dao.soldier(m.soldierId);
            if (c != null && s != null && c.uid != null && s.uid != null) {
                dao.deleteTombstone("member_" + memberUid(c.uid, s.uid));
            }
        }
        dao.addMembers(list);
    }
}
