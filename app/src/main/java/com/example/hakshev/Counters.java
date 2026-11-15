package com.example.hakshev;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Builds the rows and text reports for counters. Everything here runs on {@link Db#IO}. */
public final class Counters {
    private Counters() {}

    /** One line on screen: a soldier inside a counter. */
    public static class Row {
        public long counterId;
        public long soldierId;
        public String title = "";
        public String sub = "";
        public String valueText = "";
        public boolean reached;
        public int value;
        public int target;
        /** What to show in the colored circle: soldier name or counter name. */
        public String avatarText = "";
    }

    public static String valueText(CounterDef def, int value) {
        String s = def.target > 0 ? value + "/" + def.target : String.valueOf(value);
        return def.unit.isEmpty() ? s : s + " " + def.unit;
    }

    private static String previousText(Context c, CounterDef def, int prevValue) {
        return c.getString(R.string.previous_period, valueText(def, prevValue));
    }

    /** Rows for every member of a counter (counter screen). */
    public static List<Row> memberRows(Context c, CounterDef def, List<Soldier> members, long now) {
        AppDao dao = Db.dao(c);
        long[] cur = Periods.range(def.period, def.createdAt, now, 0);
        Map<Long, Integer> curMap = toMap(dao.totals(def.id, cur[0], cur[1]));
        Map<Long, Integer> prevMap = null;
        if (!Periods.TOTAL.equals(def.period)) {
            long[] prev = Periods.range(def.period, def.createdAt, now, -1);
            prevMap = toMap(dao.totals(def.id, prev[0], prev[1]));
        }
        List<Row> rows = new ArrayList<>();
        for (Soldier s : members) {
            int v = get(curMap, s.id);
            Row r = new Row();
            r.counterId = def.id;
            r.soldierId = s.id;
            r.title = s.fullName;
            r.avatarText = s.fullName;
            r.value = v;
            r.target = def.target;
            r.valueText = valueText(def, v);
            r.reached = def.target > 0 && v >= def.target;
            if (prevMap != null) r.sub = previousText(c, def, get(prevMap, s.id));
            rows.add(r);
        }
        return rows;
    }

    /** Rows for every counter a soldier takes part in (soldier screen). */
    public static List<Row> soldierRows(Context c, Soldier s, List<CounterDef> counters, long now) {
        AppDao dao = Db.dao(c);
        List<Row> rows = new ArrayList<>();
        for (CounterDef def : counters) {
            long[] cur = Periods.range(def.period, def.createdAt, now, 0);
            int v = dao.total(def.id, s.id, cur[0], cur[1]);
            Row r = new Row();
            r.counterId = def.id;
            r.soldierId = s.id;
            r.title = def.name + " (" + Periods.label(c, def.period) + ")";
            r.avatarText = def.name;
            r.value = v;
            r.target = def.target;
            r.valueText = valueText(def, v);
            r.reached = def.target > 0 && v >= def.target;
            if (!Periods.TOTAL.equals(def.period)) {
                long[] prev = Periods.range(def.period, def.createdAt, now, -1);
                r.sub = previousText(c, def, dao.total(def.id, s.id, prev[0], prev[1]));
            }
            rows.add(r);
        }
        return rows;
    }

    /**
     * Saves entries and applies conversion rules (e.g. 3 of "A" become 1 of "B"). Runs the work
     * on {@link Db#IO}, shows a message for every conversion, then runs {@code afterOnIo}
     * (still on the IO thread) so the caller can reload.
     */
    public static void addEntries(Activity a, List<CounterEntry> entries, Runnable afterOnIo) {
        Db.IO.execute(() -> {
            StringBuilder msgs = new StringBuilder();
            for (CounterEntry e : entries) {
                String m = addEntry(a, e);
                if (m != null) msgs.append(m).append("\n");
            }
            if (msgs.length() > 0) {
                String text = msgs.toString().trim();
                a.runOnUiThread(() -> Toast.makeText(a, text, Toast.LENGTH_LONG).show());
            }
            if (afterOnIo != null) afterOnIo.run();
        });
    }

    /** IO thread. Returns a message when a conversion happened, otherwise null. */
    private static String addEntry(Context c, CounterEntry e) {
        AppDao dao = Db.dao(c);
        e.updatedAt = System.currentTimeMillis();
        dao.insertEntry(e);
        if (e.delta <= 0) return null;
        CounterDef def = dao.counter(e.counterId);
        if (def == null || def.convertAt <= 0 || def.convertToId == 0 || def.convertToId == def.id) return null;
        CounterDef target = dao.counter(def.convertToId);
        if (target == null) return null;

        long[] r = Periods.range(def.period, def.createdAt, e.createdAt, 0);
        int total = dao.total(def.id, e.soldierId, r[0], r[1]);
        int conversions = 0;
        while (total >= def.convertAt) {
            CounterEntry out = new CounterEntry();
            out.counterId = def.id;
            out.soldierId = e.soldierId;
            out.delta = -def.convertAt;
            out.note = c.getString(R.string.note_converted_to, target.name);
            out.recordedBy = e.recordedBy;
            out.createdAt = e.createdAt;
            out.updatedAt = System.currentTimeMillis();
            dao.insertEntry(out);

            CounterEntry in = new CounterEntry();
            in.counterId = target.id;
            in.soldierId = e.soldierId;
            in.delta = 1;
            in.note = c.getString(R.string.note_converted_from, def.convertAt, def.name);
            in.recordedBy = e.recordedBy;
            in.createdAt = e.createdAt;
            in.updatedAt = System.currentTimeMillis();
            dao.insertEntry(in);

            total -= def.convertAt;
            conversions++;
        }
        if (conversions == 0) return null;
        List<CounterMember> membership = new ArrayList<>();
        membership.add(new CounterMember(target.id, e.soldierId));
        Track.addMembers(dao, membership);
        Soldier s = dao.soldier(e.soldierId);
        return c.getString(R.string.converted_message, s == null ? "" : s.fullName,
                def.convertAt * conversions, def.name, conversions, target.name);
    }

    /** Sum over the members of the counter in the current period. */
    public static int groupTotal(Context c, CounterDef def, long now) {
        AppDao dao = Db.dao(c);
        long[] cur = Periods.range(def.period, def.createdAt, now, 0);
        Map<Long, Integer> map = toMap(dao.totals(def.id, cur[0], cur[1]));
        int sum = 0;
        for (Soldier s : dao.members(def.id)) sum += get(map, s.id);
        return sum;
    }

    /** WhatsApp text for one counter. */
    public static String report(Context c, CounterDef def, long now) {
        List<Soldier> members = Db.dao(c).members(def.id);
        List<Row> rows = memberRows(c, def, members, now);
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(def.name).append("* (")
                .append(Periods.label(c, def.period)).append(")\n");
        sb.append(Periods.rangeText(c, def, now)).append("\n");
        if (rows.isEmpty()) sb.append(c.getString(R.string.no_members));
        for (Row r : rows) sb.append("- ").append(r.title).append(": ").append(r.valueText).append("\n");
        return sb.toString().trim();
    }

    public static String reportAll(Context c, long now) {
        StringBuilder sb = new StringBuilder();
        for (CounterDef def : Db.dao(c).counters()) {
            sb.append(report(c, def, now)).append("\n\n");
        }
        return sb.toString().trim();
    }

    private static Map<Long, Integer> toMap(List<SoldierTotal> list) {
        Map<Long, Integer> m = new HashMap<>();
        for (SoldierTotal t : list) m.put(t.soldierId, t.total);
        return m;
    }

    private static int get(Map<Long, Integer> m, long id) {
        Integer v = m.get(id);
        return v == null ? 0 : v;
    }
}
