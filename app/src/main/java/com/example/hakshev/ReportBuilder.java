package com.example.hakshev;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds WhatsApp-friendly text reports (uses *bold* markup). */
public final class ReportBuilder {
    private ReportBuilder() {}

    private static String stamp() {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
    }

    public static String headcount(Context c, List<Soldier> soldiers) {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(c.getString(R.string.report_headcount_title)).append("*\n");
        sb.append(stamp()).append("\n\n");
        sb.append(c.getString(R.string.report_total_present,
                Alerts.headcount(soldiers), soldiers.size())).append("\n\n");

        Map<String, int[]> bySquad = new LinkedHashMap<>();
        for (Soldier s : soldiers) {
            String key = s.squad.isEmpty() ? c.getString(R.string.no_squad) : s.squad;
            int[] v = bySquad.get(key);
            if (v == null) {
                v = new int[2];
                bySquad.put(key, v);
            }
            v[1]++;
            if (Status.onSite(s.status)) v[0]++;
        }
        for (Map.Entry<String, int[]> e : bySquad.entrySet()) {
            sb.append("- ").append(e.getKey()).append(": ")
                    .append(e.getValue()[0]).append("/").append(e.getValue()[1]).append("\n");
        }

        StringBuilder away = new StringBuilder();
        for (Soldier s : soldiers) {
            if (!Status.onSite(s.status)) {
                away.append("- ").append(s.fullName).append(" (")
                        .append(Status.label(c, s.status)).append(")\n");
            }
        }
        if (away.length() > 0) {
            sb.append("\n*").append(c.getString(R.string.report_not_present)).append("*\n").append(away);
        }
        return sb.toString().trim();
    }

    public static String hydration(Context c, List<Soldier> soldiers) {
        int target = Prefs.waterTarget(c);
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(c.getString(R.string.report_hydration_title)).append("*\n");
        sb.append(stamp()).append("\n\n");
        sb.append(c.getString(R.string.report_water_avg,
                String.format(Locale.getDefault(), "%.1f", Alerts.averageWater(soldiers)), target)).append("\n");
        StringBuilder behind = new StringBuilder();
        for (Soldier s : soldiers) {
            if (s.waterCount < target && !Status.LEAVE.equals(s.status)) {
                behind.append("- ").append(s.fullName).append(": ")
                        .append(s.waterCount).append("/").append(target).append("\n");
            }
        }
        if (behind.length() > 0) {
            sb.append("\n*").append(c.getString(R.string.report_below_target)).append("*\n").append(behind);
        }
        return sb.toString().trim();
    }

    public static String morning(Context c, List<Soldier> soldiers) {
        StringBuilder sb = new StringBuilder();
        sb.append(headcount(c, soldiers)).append("\n\n");
        sb.append(hydration(c, soldiers));
        List<String> alerts = Alerts.compute(c, soldiers);
        if (!alerts.isEmpty()) {
            sb.append("\n\n*").append(c.getString(R.string.report_alerts)).append("*\n");
            for (String a : alerts) sb.append("- ").append(a).append("\n");
        }
        return sb.toString().trim();
    }
}
