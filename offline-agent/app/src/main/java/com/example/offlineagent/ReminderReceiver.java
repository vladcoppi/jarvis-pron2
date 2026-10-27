package com.example.offlineagent;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL = "offline_agent_reminders";
    @Override public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        if (title == null || title.trim().isEmpty()) title = "У тебя есть напоминание";
        int id = intent.getIntExtra("id", (int)System.currentTimeMillis());
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Напоминания помощника", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Локальные напоминания по расписанию"); ch.enableVibration(true); nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(context, CHANNEL) : new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Напоминание").setContentText(title).setStyle(new Notification.BigTextStyle().bigText(title)).setContentIntent(content).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER).setPriority(Notification.PRIORITY_HIGH).setSound(sound).setVibrate(new long[]{0,500,250,500});
        nm.notify(id, b.build());
    }
}
