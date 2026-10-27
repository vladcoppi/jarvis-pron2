package com.example.offlineagent;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && !"android.intent.action.TIME_SET".equals(intent.getAction()) && !"android.intent.action.TIMEZONE_CHANGED".equals(intent.getAction())) return;
        MainActivity.DB db = new MainActivity.DB(context);
        SQLiteDatabase sql = db.getReadableDatabase();
        Cursor c = sql.rawQuery("SELECT id,title,at_ms FROM reminders WHERE at_ms>? ORDER BY at_ms", new String[]{String.valueOf(System.currentTimeMillis())});
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        while (c.moveToNext()) {
            int id = c.getInt(0); String title = c.getString(1); long at = c.getLong(2);
            Intent alarm = new Intent(context, ReminderReceiver.class); alarm.putExtra("id", id); alarm.putExtra("title", title);
            PendingIntent pi = PendingIntent.getBroadcast(context, id, alarm, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            if (am != null) { if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi); else am.setExact(AlarmManager.RTC_WAKEUP, at, pi); }
        }
        c.close(); db.close();
    }
}
