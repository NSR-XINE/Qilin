package com.nsr.qilin

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.nsr.qilin.data.db.AppDatabase
import com.nsr.qilin.domain.model.CountdownCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CountdownWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (ACTION_UPDATE == intent.action) {
            val title = intent.getStringExtra(KEY_TITLE)
            val countdown = intent.getStringExtra(KEY_COUNTDOWN)
            val episode = intent.getStringExtra(KEY_EPISODE)
            if (title != null && countdown != null) {
                pushWidgetData(context, title, countdown, episode ?: "")
            }
        }
        super.onReceive(context, intent)
    }
    companion object {
        private const val PREFS_NAME = "widget_data"
        private const val KEY_TITLE = "widget_title"
        private const val KEY_COUNTDOWN = "widget_countdown"
        private const val KEY_EPISODE = "widget_episode"
        const val ACTION_UPDATE = "com.nsr.qilin.UPDATE_WIDGET"

        @JvmStatic
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.countdown_widget_layout)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val title = prefs.getString("${KEY_TITLE}_$appWidgetId", null)
            val countdown = prefs.getString("${KEY_COUNTDOWN}_$appWidgetId", null)
            val episode = prefs.getString("${KEY_EPISODE}_$appWidgetId", null)

            if (title != null && countdown != null) {
                views.setTextViewText(R.id.widget_show_title, title)
                views.setTextViewText(R.id.widget_countdown, countdown)
                views.setTextViewText(R.id.widget_episode, episode ?: "")

                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = android.app.PendingIntent.getActivity(
                    context, 0, mainIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                views.setOnClickPendingIntent(R.id.widget_show_title, pendingIntent)
                views.setOnClickPendingIntent(R.id.widget_countdown, pendingIntent)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } else {
                // No cached data — fetch from Room on a background thread using goAsync() so the
                // BroadcastReceiver is not killed before the coroutine finishes.
                // Note: goAsync() is not available on AppWidgetProvider directly; we use a
                // standalone IO coroutine and commit the final RemoteViews update on completion.
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val shows = db.showDao().getAllShowsList().map { it.toDomain() }
                        val nextUp = CountdownCalculator.getNextUpShow(shows)

                        if (nextUp != null) {
                            val targetMs = CountdownCalculator.getNextReleaseDateMs(nextUp.releaseDay, nextUp.releaseTime)
                            val remaining = CountdownCalculator.calculateTimeRemaining(targetMs)
                            val countdownStr = if (remaining.airingNow) {
                                "AIRING NOW"
                            } else {
                                "${remaining.days}d ${remaining.hours}h ${remaining.minutes}m"
                            }
                            val epStr = "Episode ${nextUp.currentEp + 1} releasing ${nextUp.releaseDay}"

                            views.setTextViewText(R.id.widget_show_title, nextUp.title)
                            views.setTextViewText(R.id.widget_countdown, countdownStr)
                            views.setTextViewText(R.id.widget_episode, epStr)
                        } else {
                            views.setTextViewText(R.id.widget_show_title, "No shows yet")
                            views.setTextViewText(R.id.widget_countdown, "--d --h --m")
                            views.setTextViewText(R.id.widget_episode, "Add a show to get started")
                        }
                    } catch (e: Exception) {
                        views.setTextViewText(R.id.widget_show_title, "No shows yet")
                        views.setTextViewText(R.id.widget_countdown, "--d --h --m")
                        views.setTextViewText(R.id.widget_episode, "Add a show to get started")
                    }

                    val mainIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        context, 0, mainIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_show_title, pendingIntent)
                    views.setOnClickPendingIntent(R.id.widget_countdown, pendingIntent)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        @JvmStatic
        fun pushWidgetData(context: Context, title: String, countdown: String, episode: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, CountdownWidget::class.java))

            for (id in ids) {
                editor.putString("${KEY_TITLE}_$id", title)
                editor.putString("${KEY_COUNTDOWN}_$id", countdown)
                editor.putString("${KEY_EPISODE}_$id", episode)
            }
            editor.apply()

            val intent = Intent(context, CountdownWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
