package com.example.hakshev;

import android.content.Context;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** Computes the "exceptions" list shown on the dashboard and in reports. */
public final class Alerts {
    private Alerts() {}

    /** Water cups a soldier should have had by now (06:00-20:00 window). */
    public static int expectedWater(int target) {
        Calendar cal = Calendar.getInstance();
        double hour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0;
        double progress = Math.max(0, Math.min(1, (hour - 6) / 14.0));
        return (int) Math.floor(target * progress);
    }

    public static List<String> compute(Context c, List<Soldier> soldiers) {
        List<String> out = new ArrayList<>();
        int target = Prefs.waterTarget(c);
        int expected = expectedWater(target);
        long now = System.currentTimeMillis();
        long tomorrow = now + 24L * 3600 * 1000;
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        for (Soldier s : soldiers) {
            if (Status.LEAVE.equals(s.status)) continue;
            if (expected - s.waterCount >= 2) {
                out.add(c.getString(R.string.alert_water_behind, s.fullName, s.waterCount, expected));
            }
            if (s.exemptionExpiry > 0 && s.exemptionExpiry <= tomorrow) {
                out.add(c.getString(R.string.alert_exemption, s.fullName, df.format(new Date(s.exemptionExpiry))));
            }
        }
        return out;
    }

    public static double averageWater(List<Soldier> soldiers) {
        if (soldiers.isEmpty()) return 0;
        int sum = 0;
        for (Soldier s : soldiers) sum += s.waterCount;
        return sum / (double) soldiers.size();
    }

    public static int headcount(List<Soldier> soldiers) {
        int n = 0;
        for (Soldier s : soldiers) if (Status.onSite(s.status)) n++;
        return n;
    }
}
