package com.example.offlineagent;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;
import java.util.*;

public class MainActivity extends Activity {
    DB db; LinearLayout root, content; EditText command; TextView status;
    @Override public void onCreate(Bundle b) { super.onCreate(b); db=new DB(this); showHome(); }
    int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    TextView text(String s,int size,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(0xff172033); if(bold)t.setTypeface(null,Typeface.BOLD); t.setPadding(dp(2),dp(6),dp(2),dp(6)); return t; }
    Button button(String s,Runnable r){ Button b=new Button(this); b.setText(s); b.setOnClickListener(v->r.run()); return b; }
    void base(String title){
        ScrollView sc=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(1); root.setPadding(dp(16),dp(12),dp(16),dp(20)); root.setBackgroundColor(0xfff5f7fb); sc.addView(root); setContentView(sc);
        TextView h=text("◉  OFFLINE AI AGENT",18,true); h.setTextColor(0xff2448b8); root.addView(h); root.addView(text(title,24,true));
    }
    void showHome(){ base("Личный помощник");
        root.addView(text("Локальные заметки и задачи сохраняются на телефоне. Для работы не нужен аккаунт.",15,false));
        command=new EditText(this); command.setHint("Например: запомни купить хлеб"); command.setMinHeight(dp(54)); root.addView(command);
        root.addView(button("Выполнить запрос",()->handle(command.getText().toString().trim())));
        root.addView(button("＋ Новая заметка",()->prompt("Новая заметка",s->{db.addNote(s);showHome();})));
        root.addView(button("＋ Новая задача",()->prompt("Название задачи",s->{db.addTask(s);showHome();})));
        root.addView(button("Мои заметки",()->showNotes())); root.addView(button("План задач",()->showTasks()));
        root.addView(button("⏰ Напоминания",()->showReminders()));
        root.addView(button("＋ Создать напоминание",()->createReminder()));
        root.addView(text("Статус: локальный режим · SQLite · напоминания без интернета",12,false));
        root.addView(text("Важно: в этой сборке планировщик работает по правилам. Языковая модель ещё не встроена.",13,true));
    }
    interface Value { void accept(String s); }
    void prompt(String title,Value done){ EditText e=new EditText(this); e.setHint("Введите текст"); new AlertDialog.Builder(this).setTitle(title).setView(e).setNegativeButton("Отмена",(d,w)->{}).setPositiveButton("Сохранить",(d,w)->{String s=e.getText().toString().trim();if(!s.isEmpty())done.accept(s);}).show(); }
    void handle(String s){ if(s.isEmpty()){toast("Введите запрос");return;} String l=s.toLowerCase(Locale.ROOT);
        if(l.startsWith("запомни ")||l.startsWith("запиши ")){String note=s.substring(s.indexOf(' ')+1).trim();db.addNote(note);toast("Сохранено в памяти");showHome();}
        else if(l.startsWith("задача ")||l.startsWith("добавь задачу ")){String task=s.substring(s.indexOf(' ')+1).trim(); if(task.toLowerCase(Locale.ROOT).startsWith("задачу "))task=task.substring(7);db.addTask(task);toast("Задача добавлена");showHome();}
        else if(l.equals("покажи заметки")||l.equals("мои заметки")){showNotes();}
        else if(l.equals("покажи задачи")||l.equals("мои задачи")){showTasks();}
        else { new AlertDialog.Builder(this).setTitle("Пока не умею понять запрос").setMessage("Попробуй команды:\n• Запомни купить хлеб\n• Добавь задачу позвонить маме\n• Покажи заметки\n• Покажи задачи\n\nСвободное понимание языка появится после интеграции локальной модели.").setPositiveButton("Понятно",(d,w)->{}).show(); }
    }
    void createReminder(){
        EditText e=new EditText(this); e.setHint("Что напомнить?");
        Calendar cal=Calendar.getInstance(); cal.add(Calendar.MINUTE,5); cal.set(Calendar.SECOND,0); cal.set(Calendar.MILLISECOND,0);
        final Calendar chosen=(Calendar)cal.clone();
        LinearLayout box=new LinearLayout(this); box.setOrientation(1); box.setPadding(dp(20),dp(8),dp(20),0); box.addView(e);
        Button when=new Button(this); when.setText("Время: "+new SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(chosen.getTime())); box.addView(when);
        when.setOnClickListener(v->{Calendar now=Calendar.getInstance(); new DatePickerDialog(this,(view,y,m,d)->{chosen.set(Calendar.YEAR,y);chosen.set(Calendar.MONTH,m);chosen.set(Calendar.DAY_OF_MONTH,d);new TimePickerDialog(this,(tv,h,min)->{chosen.set(Calendar.HOUR_OF_DAY,h);chosen.set(Calendar.MINUTE,min);chosen.set(Calendar.SECOND,0);chosen.set(Calendar.MILLISECOND,0);when.setText("Время: "+new SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(chosen.getTime()));},chosen.get(Calendar.HOUR_OF_DAY),chosen.get(Calendar.MINUTE),true).show();},chosen.get(Calendar.YEAR),chosen.get(Calendar.MONTH),chosen.get(Calendar.DAY_OF_MONTH)).show();});
        new AlertDialog.Builder(this).setTitle("Новое напоминание").setView(box).setNegativeButton("Отмена",null).setPositiveButton("Сохранить",(d,w)->{String title=e.getText().toString().trim();if(title.isEmpty()){toast("Введите текст напоминания");return;}if(chosen.getTimeInMillis()<=System.currentTimeMillis()){toast("Выбери время в будущем");return;}long id=db.addReminder(title,chosen.getTimeInMillis());scheduleReminder((int)id,title,chosen.getTimeInMillis());toast("Напоминание сохранено");showReminders();}).show();
    }
    void scheduleReminder(int id,String title,long at){
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE); Intent i=new Intent(this,ReminderReceiver.class);i.putExtra("id",id);i.putExtra("title",title);
        PendingIntent pi=PendingIntent.getBroadcast(this,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(am!=null){if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);else am.setExact(AlarmManager.RTC_WAKEUP,at,pi);}
    }
    void showReminders(){
        base("Напоминания"); Cursor c=db.getReadableDatabase().rawQuery("SELECT id,title,at_ms FROM reminders ORDER BY at_ms ASC",null);
        if(c.getCount()==0)root.addView(text("Пока нет напоминаний.",15,false));
        while(c.moveToNext()){int id=c.getInt(0);String title=c.getString(1);long at=c.getLong(2);LinearLayout row=new LinearLayout(this);row.setOrientation(1);row.setPadding(dp(10),dp(8),dp(10),dp(8));row.setBackgroundColor(0xffffffff);row.addView(text(title,16,true));row.addView(text(new SimpleDateFormat("EEE, dd.MM.yyyy · HH:mm",Locale.getDefault()).format(new Date(at)),13,false));row.addView(button("Удалить",()->new AlertDialog.Builder(this).setMessage("Удалить напоминание?").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{cancelReminder(id);db.getWritableDatabase().delete("reminders","id=?",new String[]{String.valueOf(id)});showReminders();}).show()));root.addView(row);}
        c.close();root.addView(button("＋ Создать напоминание",()->createReminder()));root.addView(button("← Назад",()->showHome()));
    }
    void cancelReminder(int id){Intent i=new Intent(this,ReminderReceiver.class);PendingIntent pi=PendingIntent.getBroadcast(this,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);if(am!=null)am.cancel(pi);pi.cancel();}
    void showNotes(){base("Память / заметки"); Cursor c=db.getReadableDatabase().rawQuery("SELECT id,body,created FROM notes ORDER BY id DESC",null); if(c.getCount()==0)root.addView(text("Пока нет заметок.",15,false)); while(c.moveToNext()){final int id=c.getInt(0); LinearLayout row=new LinearLayout(this);row.setOrientation(1);row.setPadding(dp(8),dp(8),dp(8),dp(8));row.setBackgroundColor(0xffffffff);row.addView(text(c.getString(1),16,false));row.addView(text(c.getString(2),11,false));row.addView(button("Удалить",()->new AlertDialog.Builder(this).setMessage("Удалить заметку?").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{db.getWritableDatabase().delete("notes","id=?",new String[]{""+id});showNotes();}).show()));root.addView(row); } c.close();root.addView(button("＋ Добавить заметку",()->prompt("Новая заметка",s->{db.addNote(s);showNotes();})));root.addView(button("← Назад",()->showHome())); }
    void showTasks(){base("План задач"); Cursor c=db.getReadableDatabase().rawQuery("SELECT id,title,state FROM tasks ORDER BY id DESC",null);if(c.getCount()==0)root.addView(text("Пока нет задач.",15,false));while(c.moveToNext()){int id=c.getInt(0);String title=c.getString(1),state=c.getString(2);LinearLayout row=new LinearLayout(this);row.setOrientation(1);row.setPadding(dp(8),dp(8),dp(8),dp(8));row.setBackgroundColor(0xffffffff);row.addView(text((state.equals("done")?"✓ ":"○ ")+title,16,true));row.addView(text(state.equals("done")?"Выполнено":"Ожидает выполнения",12,false));if(!state.equals("done"))row.addView(button("Выполнить",()->runTask(id,title)));row.addView(button("Удалить",()->new AlertDialog.Builder(this).setMessage("Удалить задачу?").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{db.getWritableDatabase().delete("tasks","id=?",new String[]{""+id});showTasks();}).show()));root.addView(row);}c.close();root.addView(button("＋ Добавить задачу",()->prompt("Новая задача",s->{db.addTask(s);showTasks();})));root.addView(button("← Назад",()->showHome())); }
    void runTask(int id,String title){String[] steps=makePlan(title);StringBuilder msg=new StringBuilder("План выполнения:\n\n");for(int i=0;i<steps.length;i++)msg.append(i+1).append(". ").append(steps[i]).append("\n");msg.append("\nВыполнить этот безопасный локальный план?");new AlertDialog.Builder(this).setTitle("Планировщик задач").setMessage(msg.toString()).setNegativeButton("Отмена",null).setPositiveButton("Выполнить",(d,w)->{db.addRun(id,title,steps);db.getWritableDatabase().execSQL("UPDATE tasks SET state='done' WHERE id="+id);new AlertDialog.Builder(this).setTitle("Задача обработана").setMessage("План сохранён в журнале. Эта версия выполняет только локальные операции приложения, а не действия в других приложениях.").setPositiveButton("OK",(x,y)->showTasks()).show();}).show();}
    String[] makePlan(String title){return new String[]{"Сохранить задачу в локальном журнале", "Зафиксировать цель: "+title, "Отметить выполнение и сохранить результат"};}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    static class DB extends SQLiteOpenHelper {
        DB(Context a){super(a,"agent_memory.db",null,2);}
        public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE notes(id INTEGER PRIMARY KEY AUTOINCREMENT, body TEXT NOT NULL, created TEXT NOT NULL)");d.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,state TEXT NOT NULL DEFAULT 'open',created TEXT NOT NULL)");d.execSQL("CREATE TABLE runs(id INTEGER PRIMARY KEY AUTOINCREMENT,task_id INTEGER,title TEXT,steps TEXT,created TEXT)");d.execSQL("CREATE TABLE reminders(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,at_ms INTEGER NOT NULL)");}
        public void onUpgrade(SQLiteDatabase d,int o,int n){if(o<2)d.execSQL("CREATE TABLE IF NOT EXISTS reminders(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,at_ms INTEGER NOT NULL)");}
        String now(){return new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date());}
        void addNote(String s){ContentValues v=new ContentValues();v.put("body",s);v.put("created",now());getWritableDatabase().insert("notes",null,v);}
        void addTask(String s){ContentValues v=new ContentValues();v.put("title",s);v.put("state","open");v.put("created",now());getWritableDatabase().insert("tasks",null,v);}
        long addReminder(String title,long at){ContentValues v=new ContentValues();v.put("title",title);v.put("at_ms",at);return getWritableDatabase().insert("reminders",null,v);}
        void addRun(int id,String title,String[] steps){ContentValues v=new ContentValues();v.put("task_id",id);v.put("title",title);v.put("steps",android.text.TextUtils.join("\n",steps));v.put("created",now());getWritableDatabase().insert("runs",null,v);}
    }
}
