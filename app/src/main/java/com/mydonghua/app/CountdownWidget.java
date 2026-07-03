package com.mydonghua.app;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

public class CountdownWidget extends AppWidgetProvider {

    private static final String PREFS_NAME = "widget_data";
    private static final String KEY_TITLE = "widget_title";
    private static final String KEY_COUNTDOWN = "widget_countdown";
    private static final String KEY_EPISODE = "widget_episode";
    private static final String ACTION_UPDATE = "com.mydonghua.app.UPDATE_WIDGET";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId);
        }
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.countdown_widget_layout);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String title = prefs.getString(KEY_TITLE + "_" + appWidgetId, "No shows yet");
        String countdown = prefs.getString(KEY_COUNTDOWN + "_" + appWidgetId, "--d --h --m");
        String episode = prefs.getString(KEY_EPISODE + "_" + appWidgetId, "");

        views.setTextViewText(R.id.widget_show_title, title);
        views.setTextViewText(R.id.widget_countdown, countdown);
        if (episode.isEmpty()) {
            views.setTextViewText(R.id.widget_episode, "Add a show to get started");
        } else {
            views.setTextViewText(R.id.widget_episode, episode);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static void pushWidgetData(Context context, String title, String countdown, String episode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(
            new android.content.ComponentName(context, CountdownWidget.class));

        for (int id : ids) {
            editor.putString(KEY_TITLE + "_" + id, title);
            editor.putString(KEY_COUNTDOWN + "_" + id, countdown);
            editor.putString(KEY_EPISODE + "_" + id, episode);
        }
        editor.apply();

        Intent intent = new Intent(context, CountdownWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_UPDATE.equals(intent.getAction())) {
            String title = intent.getStringExtra(KEY_TITLE);
            String countdown = intent.getStringExtra(KEY_COUNTDOWN);
            String episode = intent.getStringExtra(KEY_EPISODE);
            if (title != null && countdown != null) {
                pushWidgetData(context, title, countdown, episode != null ? episode : "");
            }
        }
        super.onReceive(context, intent);
    }
}
